package com.anatawa12.jarInJar.creator.classPatch;

import com.anatawa12.jarInJar.creator.Constants;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;
import java.util.function.Function;

import static com.anatawa12.jarInJar.creator.Constants.iClass;
import static com.anatawa12.jarInJar.creator.Constants.iObject;
import static com.anatawa12.jarInJar.creator.Constants.iString;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.T_BYTE;

public final class PostConstantsPatcher extends Patcher {
    public static final PostConstantsPatcher INSTANCE = new PostConstantsPatcher();

    private PostConstantsPatcher() {
    }

    @Override
    public ClassNode patch(ClassNode node, ClassPatchParam param) {
        for (MethodNode method : node.methods) {
            patch(method, node, param);
        }
        return node;
    }

    private void patch(MethodNode method, ClassNode classNode, ClassPatchParam param) {
        InsnList list = method.instructions;
        if (list == null) throw new IllegalStateException();
        for (AbstractInsnNode insnNode : list) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if (methodInsnNode.owner.equals(Constants.slashedPostConstantsName)) {
                    switch (methodInsnNode.name) {
                        case "getFMLInjectionData":
                            patchGetFMLInjectionData(methodInsnNode, list, param);
                            break;
                        case "getModSha256":
                            patchGetModSha256(methodInsnNode, list, param);
                            break;
                        case "dottedFMLName":
                            patchDottedFMLName(methodInsnNode, list, classNode, param);
                            break;
                        case "slashedFMLName":
                            patchSlashedFMLName(methodInsnNode, list, classNode, param);
                            break;
                        default:
                            throw new IllegalStateException("unknown VersionedPart method " + methodInsnNode.name);
                    }
                }
            }
            if (insnNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                if (fieldInsnNode.owner.equals(Constants.slashedPostConstantsName)) {
                    switch (fieldInsnNode.name) {
                        case "CoreModManagerClass":
                            patchGettingClass(fieldInsnNode, list, "relauncher/CoreModManager", param);
                            break;
                        case "ModDiscovererClass":
                            patchGettingClass(fieldInsnNode, list, "common/discovery/ModDiscoverer", param);
                            break;
                        default:
                            throw new IllegalStateException("unknown VersionedPart method " + fieldInsnNode.name);
                    }
                }
            }
        }
    }

    private void patchGetFMLInjectionData(MethodInsnNode methodInsnNode, InsnList list, ClassPatchParam param) {
        checkMethod(methodInsnNode, "()[L" + iObject + ';');

        list.set(methodInsnNode, new MethodInsnNode(INVOKESTATIC,
                param.target.slashedFMLName("relauncher/FMLInjectionData"),
                "data", "()[L" + iObject + ';', false));
    }

    private void patchGetModSha256(MethodInsnNode methodInsnNode, InsnList list, ClassPatchParam param) {
        checkMethod(methodInsnNode, "()[B");
        AbstractInsnNode next = methodInsnNode.getNext();
        list.remove(methodInsnNode);

        byte[] sha256JarHash = param.sha256JarHash;
        list.insertBefore(next, new IntInsnNode(BIPUSH, sha256JarHash.length));
        list.insertBefore(next, new IntInsnNode(NEWARRAY, T_BYTE));

        for (int i = 0; i < sha256JarHash.length; i++) {
            list.insertBefore(next, new InsnNode(DUP));
            list.insertBefore(next, putInt(i));
            list.insertBefore(next, putInt(sha256JarHash[i]));
            list.insertBefore(next, new InsnNode(BASTORE));
        }
    }

    private AbstractInsnNode putInt(int value) {
        if (-1 <= value && value <= 5)
            return new InsnNode(value + ICONST_0);
        else if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE)
            return new IntInsnNode(BIPUSH, value);
        else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE)
            return new IntInsnNode(SIPUSH, value);
        else
            return new LdcInsnNode(value);
    }

    private void patchDottedFMLName(MethodInsnNode methodInsnNode, InsnList list, ClassNode classNode, ClassPatchParam param) {
        checkMethod(methodInsnNode, "(L" + iString + ";)L" + iString + ';');
        replaceConstantCall(methodInsnNode, list, classNode,
                (LdcInsnNode insn) -> param.target.dottedFMLName((String) insn.cst));
    }

    private void patchSlashedFMLName(MethodInsnNode methodInsnNode, InsnList list, ClassNode classNode, ClassPatchParam param) {
        checkMethod(methodInsnNode, "(L" + iString + ";)L" + iString + ';');
        replaceConstantCall(methodInsnNode, list, classNode,
                (LdcInsnNode insn) -> param.target.slashedFMLName((String) insn.cst));
    }

    private void patchGettingClass(FieldInsnNode fieldInsnNode, InsnList list, String className, ClassPatchParam param) {
        if (fieldInsnNode.getOpcode() != GETSTATIC
                || !fieldInsnNode.desc.equals('L' + iClass + ';'))
            throw new IllegalStateException("invalid " + fieldInsnNode.name + " getting");

        list.set(fieldInsnNode, new LdcInsnNode(Type.getObjectType(param.target.slashedFMLName(className))));
    }

    private void replaceConstantCall(MethodInsnNode methodInsnNode, InsnList list,
                                     ClassNode classNode, Function<LdcInsnNode, Object> computer) {
        AbstractInsnNode node = methodInsnNode.getPrevious();
        if (node.getOpcode() != LDC) throw new IllegalStateException("invalid " + methodInsnNode.name + " invocation: " +
                "must be called with constant");
        list.remove(node);
        Object value = computer.apply((LdcInsnNode)node);
        LdcInsnNode ldcInsnNode;
        list.set(methodInsnNode, ldcInsnNode = new LdcInsnNode(value));
        
        // if next insn is PUTSTATIC for static final field in this class, set constant value.
        if (ldcInsnNode.getNext().getOpcode() == PUTSTATIC) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) ldcInsnNode.getNext();
            if (fieldInsnNode.owner.equals(classNode.name)) {
                classNode.fields.stream()
                        .filter(fieldNode -> fieldNode.name.equals(fieldInsnNode.name)
                                && fieldNode.desc.equals(fieldInsnNode.desc))
                        .findFirst()
                        .ifPresent(fieldNode -> {
                            if ((fieldNode.access & (ACC_STATIC | ACC_FINAL)) == (ACC_STATIC | ACC_FINAL)) {
                                fieldNode.value = value;
                            }
                        });
            }
        }
    }

    private void checkMethod(MethodInsnNode methodInsnNode, String desc) {
        if (methodInsnNode.getOpcode() != INVOKESTATIC
                || !methodInsnNode.desc.equals(desc))
            throw new IllegalStateException("invalid " + methodInsnNode.name + " invocation");
    }
}
