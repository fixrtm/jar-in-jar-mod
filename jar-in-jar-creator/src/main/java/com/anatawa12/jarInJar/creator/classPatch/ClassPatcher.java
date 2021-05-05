package com.anatawa12.jarInJar.creator.classPatch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public final class ClassPatcher {
    private static final Patcher[] patchers = new Patcher[]{
            PostConstantsPatcher.INSTANCE,
            ModCandidatePatcher.INSTANCE,
            RemappingPatcher.INSTANCE,
            ResolveSimpleConstantsPatcher.INSTANCE,
            ResolveStringJoinConstantPatcher.INSTANCE,
    };

    private ClassPatcher() {
    }

    public static byte[] modify(byte[] inputClass, ClassPatchParam param) {
        ClassNode node = readToClassNode(inputClass);
        for (Patcher patcher : patchers)
            node = patcher.patch(node, param);
        return writeClassNode(node);
    }

    private static ClassNode readToClassNode(byte[] inputClass) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(inputClass);
        reader.accept(node, 0);
        return node;
    }

    private static byte[] writeClassNode(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

}
