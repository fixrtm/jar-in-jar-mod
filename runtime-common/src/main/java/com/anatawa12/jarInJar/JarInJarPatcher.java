package com.anatawa12.jarInJar;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static com.anatawa12.jarInJar.PostConstants.dottedFMLName;
import static com.anatawa12.jarInJar.PostConstants.slashedFMLName;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class JarInJarPatcher implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        assert JarInJarModLoader.isLatest();
        if (dottedFMLName("common.Loader").equals(name))
            return patchLoader(basicClass);
        return basicClass;
    }

    private static final String iModDiscoverer = slashedFMLName("common/discovery/ModDiscoverer");

    /*
    insert
        ALOAD $discover
        INVOKESTATIC JarInJarModLoader.identifyMods(ModDiscoverer)
    before
        ALOAD $this
        GETFIELD Loader.mods
        ALOAD $discover
        INVOKEVIRTUAL ModDiscoverer.identifyMods()
       #INVOKEVIRTUAL List.addAll(Iterable)
     */
    private byte[] patchLoader(byte[] basicClass) {
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);

        MethodNode identifyMods = node.methods.stream()
                .filter(it -> it.name.equals("identifyMods") && (
                        it.desc.equals("(Ljava/util/List;)L" + iModDiscoverer + ";") 
                                ||  it.desc.equals("()L" + iModDiscoverer + ";")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("identifyMods not found"));

        InsnList insns = identifyMods.instructions;
        AbstractInsnNode callIdentifyMods = insns.getFirst();
        while (!(callIdentifyMods.getOpcode() == INVOKEVIRTUAL
                && ((MethodInsnNode) callIdentifyMods).owner.equals(iModDiscoverer)
                && ((MethodInsnNode) callIdentifyMods).name.equals("identifyMods")
                && ((MethodInsnNode) callIdentifyMods).desc.equals("()Ljava/util/List;")))
            callIdentifyMods = callIdentifyMods.getNext();

        AbstractInsnNode loadDiscoverer = prevInsn(callIdentifyMods);
        AbstractInsnNode getFieldMods = prevInsn(loadDiscoverer);
        AbstractInsnNode loadThis = prevInsn(getFieldMods);
        if (loadDiscoverer.getOpcode() != ALOAD)
            throw new AssertionError("unknown code fragment in identifyMods");
        if (getFieldMods.getOpcode() != GETFIELD)
            throw new AssertionError("unknown code fragment in identifyMods");
        if (loadThis.getOpcode() != ALOAD)
            throw new AssertionError("unknown code fragment in identifyMods");
        int discoverer = ((VarInsnNode)loadDiscoverer).var;

        insns.insertBefore(loadThis, new VarInsnNode(ALOAD, discoverer));
        insns.insertBefore(loadThis, new MethodInsnNode(INVOKESTATIC, Type.getInternalName(JarInJarModLoader.class), 
                "identifyMods", "(Ljava/lang/Object;)V", false));

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private AbstractInsnNode prevInsn(AbstractInsnNode insn) {
        do insn = insn.getPrevious();
        while (insn.getOpcode() == -1);
        return insn;
    }
}
