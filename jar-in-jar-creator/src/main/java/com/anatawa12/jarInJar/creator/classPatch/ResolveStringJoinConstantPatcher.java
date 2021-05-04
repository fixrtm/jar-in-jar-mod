package com.anatawa12.jarInJar.creator.classPatch;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static com.anatawa12.jarInJar.creator.Constants.iString;
import static com.anatawa12.jarInJar.creator.Constants.iStringBuilder;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.SIPUSH;

public final class ResolveStringJoinConstantPatcher extends Patcher {
    public static final ResolveStringJoinConstantPatcher INSTANCE = new ResolveStringJoinConstantPatcher();

    @Override
    public ClassNode patch(ClassNode node, ClassPatchParam param) {
        for (MethodNode method : node.methods) {
            InsnList insnList = method.instructions;
            if (insnList == null) continue;
            for (AbstractInsnNode insnNode : insnList) {
                patchInsn(insnNode, insnList, node);
            }
        }
        return node;
    }

    /*
    new StringBuilder
    invokespecial StringBuilder <init> (String)V
    for ($value in $values) {
        ldc $value
        invaokevirtual StringBuilder append ($ValueType)Lj/l/StringBuilder;
    }
    invaokevirtual StringBuilder toString ()Lj/l/String;
     */
    private void patchInsn(final AbstractInsnNode insnNode, InsnList insnList, ClassNode classNode) {
        // INVOKESTATIC: Replace Class#getName()

        if (insnNode.getOpcode() != INVOKEVIRTUAL) return;
        MethodInsnNode methodInsn = (MethodInsnNode) insnNode;
        if (methodInsn.owner.equals(iStringBuilder)
                && methodInsn.name.equals("toString")
                && methodInsn.desc.equals("()L" + iString + ';')) {
            StringBuilder resultBuilder = new StringBuilder();
            AbstractInsnNode insn;
            while ((insn = prevInsn(methodInsn)).getOpcode() == INVOKEVIRTUAL) {
                MethodInsnNode appendCall = (MethodInsnNode) insn;
                if (!appendCall.owner.equals(iStringBuilder)) return;
                if (!appendCall.name.equals("append")) return;
                Type desc = Type.getMethodType(appendCall.desc);
                Type[] params = desc.getArgumentTypes();
                if (params.length != 1) return;
                Type valueType = params[0];

                insn = prevInsn(methodInsn);
                Object constant = getConstantOf(insn);
                if (constant == NONE_VALUE) return;

                assert constant != null || valueType.getSort() == Type.OBJECT || valueType.getSort() == Type.ARRAY;
                //INT, FLOAT, LONG, DOUBLE
                switch (valueType.getSort()) {
                    case Type.BYTE:
                        constant = (byte)(int)constant;
                        break;
                    case Type.SHORT:
                        constant = (short)(int)constant;
                        break;
                    case Type.CHAR:
                        constant = (char)(int)constant;
                        break;
                    case Type.BOOLEAN:
                        constant = ((int)constant) != 0;
                        break;
                }
                resultBuilder.append(constant);
            }
            if (insn.getOpcode() != INVOKESPECIAL) return;
            MethodInsnNode callCtor = (MethodInsnNode) insn;
            if (!callCtor.owner.equals(iStringBuilder)) return;
            if (!callCtor.name.equals("<init>")) return;
            if (!callCtor.desc.equals("()V")) return;
            insn = prevInsn(methodInsn);
            if (insn.getOpcode() != NEW) return;
            TypeInsnNode newStringBuilder = (TypeInsnNode) insn;
            if (!newStringBuilder.desc.equals(iStringBuilder)) return;
            for (insn = insnNode; insn != newStringBuilder; insn = insn.getPrevious())
                insnList.remove(insn);
            insnList.set(newStringBuilder, new LdcInsnNode(resultBuilder.toString()));
        }
    }

    @SuppressWarnings("UnnecessaryBoxing")
    private Object getConstantOf(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            //@formatter:off
            case ACONST_NULL: return null;
            case ICONST_M1:   return Integer.valueOf(-1);
            case ICONST_0:    return Integer.valueOf(0);
            case ICONST_1:    return Integer.valueOf(1);
            case ICONST_2:    return Integer.valueOf(2);
            case ICONST_3:    return Integer.valueOf(3);
            case ICONST_4:    return Integer.valueOf(4);
            case ICONST_5:    return Integer.valueOf(5);
            case LCONST_0:    return Long   .valueOf(0);
            case LCONST_1:    return Long   .valueOf(1);
            case FCONST_0:    return Float  .valueOf(0);
            case FCONST_1:    return Float  .valueOf(1);
            case FCONST_2:    return Float  .valueOf(2);
            case DCONST_0:    return Double .valueOf(0);
            case DCONST_1:    return Double .valueOf(1);
            //@formatter:on
            case BIPUSH:
            case SIPUSH:
                return Integer.valueOf(((IntInsnNode) insn).operand);
            case LDC:
                return ((LdcInsnNode) insn).cst;
        }
        return NONE_VALUE;
    }

    private AbstractInsnNode prevInsn(AbstractInsnNode insn) {
        do insn = insn.getPrevious();
        while (insn.getOpcode() == -1);
        return insn;
    }

    private static final Object NONE_VALUE = new Object();
}
