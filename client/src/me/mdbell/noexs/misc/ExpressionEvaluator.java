package me.mdbell.noexs.misc;

import me.mdbell.util.Collections;

import java.util.*;

public class ExpressionEvaluator {

    public interface VariableProvider {
        long get(String name);

        boolean containsVar(String value);
    }

    public interface MemoryProvider {
        long get(long addr);
    }

    private enum Type {
        DEREF_START, DEREF_END, ARITHMETIC, CONSTANT, WHITESPACE, VARIABLE
    }

    private class Token {
        Token prev, next;
        Type type;
        Object value;

        @Override
        public String toString() {
            return "Token{" +
                    "type=" + type +
                    ", value=" + value +
                    '}';
        }
    }

    private final VariableProvider vars;
    private final MemoryProvider mem;

    public ExpressionEvaluator(VariableProvider variables, MemoryProvider mem) {
        this.vars = variables;
        this.mem = mem;
    }

    public long eval(String str) {
        List<Token> tokens = setup(str);
        Stack<Long> values = new Stack<>();
        Stack<Character> ops = new Stack<>();
        for (Token t : tokens) {
            switch (t.type) {
                case VARIABLE:
                    values.push(vars.get(t.value.toString()));
                    break;
                case CONSTANT:
                    values.push((Long) t.value);
                    break;
                case ARITHMETIC:
                case DEREF_START:
                    ops.push((Character) t.value);
                    break;
                case DEREF_END:
                    while (ops.peek() != '[') {
                        values.push(evalOp(ops.pop(), values.pop(), values.pop()));
                    }
                    ops.pop();
                    long addr = values.pop();
                    values.push(mem.get(addr));
            }
        }
        while (!ops.isEmpty()) {
            values.push(evalOp(ops.pop(), values.pop(), values.pop()));
        }
        return values.pop();
    }

    private List<Token> setup(String str) {
        Iterator<Character> itr = Collections.iterator(Objects.requireNonNull(str));
        //parse into tokens
        List<Token> tokens = getTokens(itr);
        //verify the tokens
        verifyFormatting(tokens);
        //if valid format, set tokens
        return tokens;
    }

    private Long evalOp(Character type, Long v1, Long v2) {
        switch (type) {
            case '-':
                return v2 - v1;
            case '+':
                return v2 + v1;
        }
        throw new UnsupportedOperationException("op:" + type);
    }

    private void verifyFormatting(List<Token> tokens) {
        int derefStart = 0;
        int derefEnd = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            switch (t.type) {
                case DEREF_START:
                    derefStart++;
                    break;
                case DEREF_END:
                    derefEnd++;
                    break;
                case VARIABLE:
                    if (!vars.containsVar((String)t.value)) {
                        throw new InvalidFormatException("Unknown variable:" + t.value);
                    }
                case ARITHMETIC:
                    if (t.prev == null) {
                        throw new InvalidFormatException("Missing left param for op:" + t.value);
                    }
                    if (t.next == null) {
                        throw new InvalidFormatException("Missing right param for op:" + t.value);
                    }
            }
        }
        if (derefEnd != derefStart) {
            throw new InvalidFormatException("Reference mismatch. start count:" + derefStart + " end count:" + derefEnd);
        }
    }

    private List<Token> getTokens(Iterator<Character> itr) {
        List<Token> res = new LinkedList<>();
        itr.forEachRemaining(c -> {
            Token t = new Token();
            t.type = getType(c);
            t.value = c;
            res.add(t);
        });

        //merge all consecutive variable tokens together
        for (int i = 0; i < res.size(); i++) {
            if (merge(res, i)) {
                i--;
            }
        }
        //remove whitespace
        res.removeIf(t -> t.type == Type.WHITESPACE);

        //make number types actually numbers
        res.forEach(t -> {
            if (t.type == Type.VARIABLE) {
                try {
                    t.value = Long.parseLong((String) t.value, 16);
                    t.type = Type.CONSTANT;
                } catch (NumberFormatException ignored) {
                }
            }
        });
        //finally link all tokens together
        for (int i = 0; i < res.size(); i++) {
            Token t = res.get(i);
            if (i > 0) {
                t.prev = res.get(i - 1);
            }
            if (i + 1 < res.size()) {
                t.next = res.get(i + 1);
            }
        }
        return res;
    }

    private boolean merge(List<Token> tokens, int index) {
        if (index + 1 >= tokens.size()) {
            return false;
        }
        Token curr = tokens.get(index);
        Token next = tokens.get(index + 1);
        if (curr.type != Type.VARIABLE || next.type != Type.VARIABLE) {
            return false;
        }
        tokens.remove(next);
        curr.value = curr.value.toString() + next.value.toString();
        return true;
    }

    private Type getType(char c) {
        if (Character.isWhitespace(c)) {
            return Type.WHITESPACE;
        }
        switch (c) {
            case '[':
                return Type.DEREF_START;
            case ']':
                return Type.DEREF_END;
            case '+':
            case '-':
                return Type.ARITHMETIC;
            default:
                return Type.VARIABLE;
        }
    }

    public static class InvalidFormatException extends RuntimeException {
        private InvalidFormatException(String str) {
            super(str);
        }
    }
}
