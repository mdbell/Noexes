package me.mdbell.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class PatternTokenizer {

    private class Token{
        TokenType type;
        int width;
        String value;

        @Override
        public String toString() {
            return "Token{" +
                    "type=" + type +
                    ", width=" + width +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    private enum TokenType{
        WILDCARD, WHITESPACE, OPERATOR, NUMERIC
    }

    public PatternCompiler.PatternElement[] eval(String str){
        List<Token> tokens = getTokens(str);

        List<PatternCompiler.PatternElement> res = new LinkedList<>();
        PatternCompiler.Condition condition = PatternCompiler.Condition.EQUALS;
        for(int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if(t.type == TokenType.OPERATOR) {
                switch(t.value) {
                    case "!":
                        condition = PatternCompiler.Condition.NOT_EQUALS;
                        break;
                    case "<":
                        condition = PatternCompiler.Condition.LESS_THEN;
                        break;
                    case ">":
                        condition = PatternCompiler.Condition.GREATER_THEN;
                        break;
                }
            }else{
                if(t.type == TokenType.NUMERIC) {
                    if(t.width > 2) {
                        throw new UnsupportedOperationException("Value to high for a byte!");
                    }
                    res.add(new PatternCompiler.PatternElement(Integer.parseUnsignedInt(t.value, 16), condition));
                }else if(t.type == TokenType.WILDCARD) {
                    res.add(null);
                }
                condition = PatternCompiler.Condition.EQUALS;
            }
        }
        return res.toArray(new PatternCompiler.PatternElement[0]);
    }

    private List<Token> getTokens(String str) {
        Iterator<Character> itr = Collections.iterator(Objects.requireNonNull(str));
        List<Token> res = new LinkedList<>();
        boolean ascii=false;
        while(itr.hasNext()) {
            char c = itr.next();
            if(c == '*') {
                ascii = !ascii;
                continue;
            }
            Token t = new Token();
            if(!ascii) {
                t.type = getType(c);
                t.width = 1;
                t.value = String.valueOf(c);
            }else{
                t.type = TokenType.NUMERIC;
                t.width = 2;
                t.value = HexUtils.pad('0', 2, Integer.toUnsignedString(c & 0xFF, 16));
            }
            res.add(t);
        }

        for(int i = 0; i < res.size(); i++) {
            Token t = res.get(i);
            Token next = i + 1 < res.size() ? res.get(i + 1) : null;
            if(next != null) {
                if (t.type == TokenType.NUMERIC && next.type == TokenType.NUMERIC && t.width < 2) {
                    t.value = t.value + next.value;
                    t.width++;
                    res.remove(next);
                    i--;
                }
                else if(t.type == TokenType.WILDCARD && next.type == TokenType.WILDCARD && t.width < 2) {
                    t.width++;
                    res.remove(next);
                    i--;
                }
            }
        }

        res.removeIf(t -> t.type == TokenType.WHITESPACE);
        return res;
    }

    private TokenType getType(char c) {
        if(Character.isWhitespace(c)) {
            return TokenType.WHITESPACE;
        }
        if(c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
            return TokenType.NUMERIC;
        }
        switch(c){
            case '<':
            case '>':
            case '!':
                return TokenType.OPERATOR;
            case '?':
                return TokenType.WILDCARD;
            default:
                throw new UnsupportedOperationException("unsupported/invalid char:" + c);
        }
    }
}
