package com.anatawa12.jarInJar;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class JarInJarLoaderCoreMod implements IFMLLoadingPlugin {
    static {
        JarInJarModLoader.load();
    }

    @Override
    public String[] getASMTransformerClass() {
        if (JarInJarModLoader.isLatest()) {
            return new String[]{JarInJarPatcher.class.getName()};
        } else {
            return null;
        }
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    //may @Override
    public String[] getLibraryRequestClass() {
        return new String[0];
    }

    //may @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
