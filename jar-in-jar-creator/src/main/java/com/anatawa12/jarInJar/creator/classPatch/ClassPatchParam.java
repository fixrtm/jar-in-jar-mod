package com.anatawa12.jarInJar.creator.classPatch;

import com.anatawa12.jarInJar.creator.TargetPreset;

public final class ClassPatchParam {
    public final String slashedBasePackage;
    public final TargetPreset target;
    public final byte[] sha256JarHash;

    public ClassPatchParam(String slashedBasePackage, TargetPreset target, byte[] sha256JarHash) {
        this.slashedBasePackage = slashedBasePackage;
        this.target = target;
        this.sha256JarHash = sha256JarHash;
    }
}
