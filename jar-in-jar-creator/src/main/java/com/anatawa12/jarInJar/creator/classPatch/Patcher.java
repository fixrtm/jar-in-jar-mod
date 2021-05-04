package com.anatawa12.jarInJar.creator.classPatch;

import org.objectweb.asm.tree.ClassNode;

public abstract class Patcher {
    public abstract ClassNode patch(ClassNode node, ClassPatchParam param);
}
