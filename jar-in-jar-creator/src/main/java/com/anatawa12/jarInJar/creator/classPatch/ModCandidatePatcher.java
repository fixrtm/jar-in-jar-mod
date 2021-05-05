package com.anatawa12.jarInJar.creator.classPatch;

import com.anatawa12.jarInJar.creator.Constants;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static com.anatawa12.jarInJar.creator.Constants.iFile;
import static com.anatawa12.jarInJar.creator.Constants.iString;
import static org.objectweb.asm.Opcodes.GETSTATIC;

public final class ModCandidatePatcher extends Patcher {
    public static final ModCandidatePatcher INSTANCE = new ModCandidatePatcher();

    private ModCandidatePatcher() {
    }

    @Override
    public ClassNode patch(ClassNode node, ClassPatchParam param) {
        for (MethodNode method : node.methods) {
            patch(method, param);
        }
        return node;
    }

    private void patch(MethodNode method, ClassPatchParam param) {
        InsnList list = method.instructions;
        if (list == null) throw new IllegalStateException();
        for (AbstractInsnNode insnNode : list) {
            if (!(insnNode instanceof MethodInsnNode)) continue;
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            if (!methodInsnNode.owner.equals(Constants.slashedModCandidateName)) continue;
            if (!methodInsnNode.name.equals("<init>"))
                throw new IllegalStateException("ModCandidate can be used only for constructor");
            if (!methodInsnNode.desc.equals("(L" + iFile + ";L" + iFile + ";L" + iString + ";)V"))
                throw new IllegalStateException("ModCandidate can be used only for constructor");
            LdcInsnNode ldcInsnNode = (LdcInsnNode) methodInsnNode.getPrevious();

            String iContainerType = param.target.slashedFMLName("common/discovery/ContainerType");

            list.set(ldcInsnNode, new FieldInsnNode(GETSTATIC, iContainerType,
                    ldcInsnNode.cst.toString(), 'L' + iContainerType + ';'));
            methodInsnNode.desc = "(L" + iFile + ";L" + iFile + ";L" + iContainerType + ";)V";
        }
    }
}
