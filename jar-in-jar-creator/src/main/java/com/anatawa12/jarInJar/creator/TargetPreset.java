package com.anatawa12.jarInJar.creator;

public enum TargetPreset {
    // targets 1.6.2..1.7.x
    FMLInCpw("fml-in-cpw", "cpw.mods.fml"),
    // targets 1.8 .. 1.12
    FMLInForge("fml-in-forge", "net.minecraftforge.fml"),
    ;

    public final String jarName;
    public final String packageName;

    TargetPreset(String jarName, String packageName) {
        this.jarName = jarName;
        this.packageName = packageName;
    }

    public String dottedFMLName(String inFML) {
        return packageName + '.' + inFML;
    }

    public String slashedFMLName(String inFML) {
        return dottedFMLName(inFML).replace('.', '/');
    }
}
