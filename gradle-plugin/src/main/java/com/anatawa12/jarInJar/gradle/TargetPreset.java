package com.anatawa12.jarInJar.gradle;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TargetPreset {
    // targets 1.6.2..1.7.x
    FMLInCpw("fml-in-cpw"),
    // targets 1.8 .. 1.12
    FMLInForge("fml-in-forge"),
    ;

    public String getParamName() {
        return paramName;
    }

    private final String paramName;

    TargetPreset(String paramName) {
        this.paramName = paramName;
    }

    @Override
    public String toString() {
        return paramName;
    }

    private static final Map<String, TargetPreset> byParamName = Arrays.stream(TargetPreset.values())
            .collect(Collectors.toMap(TargetPreset::getParamName, Function.identity()));

    public static TargetPreset byParamName(String targetName) {
        return byParamName.get(targetName);
    }
}
