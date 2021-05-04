package com.anatawa12.jarInJar.creator.classPatch;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static com.anatawa12.jarInJar.creator.Constants.iASMType;
import static com.anatawa12.jarInJar.creator.Constants.iClass;
import static com.anatawa12.jarInJar.creator.Constants.iString;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public final class ResolveSimpleConstantsPatcher extends Patcher {
    public static final ResolveSimpleConstantsPatcher INSTANCE = new ResolveSimpleConstantsPatcher();

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

    private void patchInsn(AbstractInsnNode insnNode, InsnList insnList, ClassNode classNode) {
        switch (insnNode.getOpcode()) {
            // INVOKESTATIC: Replace Class#getName()
            // INVOKESTATIC: Replace String#length()
            case INVOKEVIRTUAL: {
                MethodInsnNode methodInsn = (MethodInsnNode) insnNode;
                if (methodInsn.owner.equals(iClass) && methodInsn.name.equals("getName")
                        && methodInsn.desc.equals("()L" + iString + ';')) {
                    if (insnNode.getPrevious() instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insnNode.getPrevious();
                        Type type = (Type) ldc.cst;
                        insnList.remove(methodInsn);
                        ldc.cst = type.getClassName();
                    }
                } else if (methodInsn.owner.equals(iString) && methodInsn.name.equals("length")
                        && methodInsn.desc.equals("()I")) {
                    if (insnNode.getPrevious() instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insnNode.getPrevious();
                        String value = (String) ldc.cst;
                        insnList.remove(methodInsn);
                        ldc.cst = value.length();
                    }
                }
                break;
            }
            // INVOKESTATIC: Replace Type.getInternalName(Class)
            case INVOKESTATIC: {
                MethodInsnNode methodInsn = (MethodInsnNode) insnNode;
                if (methodInsn.owner.equals(iASMType) && methodInsn.name.equals("getInternalName")
                        && methodInsn.desc.equals("(L" + iClass + ";)L" + iString + ';')) {
                    if (insnNode.getPrevious() instanceof LdcInsnNode) {
                        LdcInsnNode ldc = (LdcInsnNode) insnNode.getPrevious();
                        Type type = (Type)ldc.cst;
                        insnList.remove(methodInsn);
                        ldc.cst = type.getInternalName();
                    }
                }
                break;
            }
            // GETSTATIC: Replace constant getting to ldc
            case GETSTATIC: {
                FieldInsnNode fieldInsn = (FieldInsnNode) insnNode;
                if (fieldInsn.owner.equals(classNode.name)) {
                    classNode.fields.stream()
                            .filter(fieldNode -> fieldInsn.name.equals(fieldNode.name)
                                    && fieldInsn.desc.equals(fieldNode.desc))
                            .findFirst()
                            .ifPresent(fieldNode -> {
                                if (fieldNode.value != null) {
                                    insnList.set(fieldInsn, new LdcInsnNode(fieldNode.value));
                                }
                            });
                }
            }
        }
    }
}
