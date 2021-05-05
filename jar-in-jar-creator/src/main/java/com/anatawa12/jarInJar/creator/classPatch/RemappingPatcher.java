package com.anatawa12.jarInJar.creator.classPatch;

import com.anatawa12.jarInJar.creator.TargetPreset;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import static com.anatawa12.jarInJar.creator.Constants.slashedLibraryBasePackage;
import static com.anatawa12.jarInJar.creator.Constants.slashedModCandidateName;
import static com.anatawa12.jarInJar.creator.Constants.slashedPostConstantsName;

public final class RemappingPatcher extends Patcher {
    public static final RemappingPatcher INSTANCE = new RemappingPatcher();

    private RemappingPatcher() {
    }

    @Override
    public ClassNode patch(ClassNode node, ClassPatchParam param) {
        ClassNode newNode = new ClassNode();
        node.accept(new ClassRemapper(newNode, new RemapperImpl(param)));
        return newNode;
    }

    private static class RemapperImpl extends Remapper {
        private final String slashedBasePackage;
        private final TargetPreset target;

        private RemapperImpl(ClassPatchParam param) {
            this.slashedBasePackage = param.slashedBasePackage;
            this.target = param.target;
        }

        @Override
        public String map(String internalName) {
            if (!internalName.startsWith(slashedLibraryBasePackage))
                return internalName;
            if (internalName.equals(slashedModCandidateName))
                return target.slashedFMLName("common/discovery/ModCandidate");
            if (internalName.startsWith(slashedPostConstantsName))
                throw new IllegalStateException("VersionedPart cannot be used at runtime");
            return slashedBasePackage + internalName.substring(slashedLibraryBasePackage.length());
        }
    }
}
