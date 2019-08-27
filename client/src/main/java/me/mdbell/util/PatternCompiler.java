package me.mdbell.util;

import org.objectweb.asm.*;


public class PatternCompiler{

    public enum Condition {
        EQUALS, NOT_EQUALS, GREATER_THEN, LESS_THEN
    }

    private int count = 0;
    private final PatternClassLoader loader = new PatternClassLoader();

    public static class PatternElement {
        int value;
        Condition condition;

        public PatternElement(int value) {
            this(value, Condition.EQUALS);
        }

        public PatternElement(int value, Condition condition) {
            if (condition == null) {
                condition = Condition.EQUALS;
            }
            this.value = value;
            this.condition = condition;
        }

        public int getValue() {
            return value;
        }

        public Condition getCondition() {
            return condition;
        }

        @Override
        public String toString() {
            return "PatternElement{" +
                    "value=" + value +
                    ", condition=" + condition +
                    '}';
        }
    }

    public IPatternMatcher compile(PatternElement[] pattern) {
        String name = "__Pattern" + count++;
        ClassWriter cw = createWriter(name);
        MethodVisitor mv;

        Label loopStart = new Label();
        Label loopEnd = new Label();
        Label loopContinue = new Label();
        {
            mv = createFindMethod(cw, pattern.length, loopStart, loopEnd);

            //the checks
            for (int i = 0; i < pattern.length; i++) {
                PatternElement e = pattern[i];
                if (e == null) {
                    continue;
                }
                visitTest(mv, i, e.getValue(), loopContinue, e.getCondition());
            }

            //pattern found
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitInsn(Opcodes.IRETURN);

            //pattern not found
            endFindMethod(mv, loopStart, loopEnd, loopContinue);
        }
        cw.visitEnd();

        return loader.load(name, cw.toByteArray());
    }

    private static ClassWriter createWriter(String name) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv;
        cw.visit(52, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, name, null, "java/lang/Object", new String[]{Type.getInternalName(IPatternMatcher.class)});
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return cw;
    }

    private static void visitTest(MethodVisitor mv, int index, int value, Label end, Condition condition) {
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitIntInsn(Opcodes.SIPUSH, index);
        mv.visitInsn(Opcodes.BALOAD);
        mv.visitIntInsn(Opcodes.SIPUSH, value);
        mv.visitJumpInsn(getConditionOp(condition), end);
    }

    private static int getConditionOp(Condition condition) {
        //all of these need to be inverted as they should return if the condition is not met
        switch (condition) {
            case NOT_EQUALS:
                return Opcodes.IF_ICMPEQ;
            case GREATER_THEN:
                return Opcodes.IF_ICMPLE;
            case LESS_THEN:
                return Opcodes.IF_ICMPGE;
            default:
            case EQUALS:
                return Opcodes.IF_ICMPNE;
        }
    }

    private static MethodVisitor createFindMethod(ClassWriter cw, int patternSize, Label start, Label end) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "match", "(Ljava/nio/ByteBuffer;)I", null, null);
        mv.visitCode();
        //create array to test with
        mv.visitIntInsn(Opcodes.SIPUSH, patternSize);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "remaining", "()I", false);
        mv.visitIntInsn(Opcodes.SIPUSH, patternSize);
        //check to see if we have enough bytes left to check, if not go to the end (pattern not found)
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, end);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "()I", false);
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "get", "([B)Ljava/nio/ByteBuffer;", false);
        mv.visitInsn(Opcodes.POP);
        return mv;
    }

    private static void endFindMethod(MethodVisitor mv, Label loopStart, Label loopEnd, Label loopContinue) {
        mv.visitLabel(loopContinue);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "position", "(I)Ljava/nio/Buffer;", false);
        mv.visitInsn(Opcodes.POP);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);
        mv.visitLabel(loopEnd);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private class PatternClassLoader extends ClassLoader {

        public IPatternMatcher load(String name, byte[] bytes) {
            Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
            try {
                return (IPatternMatcher) clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
