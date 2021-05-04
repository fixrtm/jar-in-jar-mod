package com.anatawa12.jarInJar.creator.commandline;

import com.anatawa12.jarInJar.creator.Logger;
import com.anatawa12.jarInJar.creator.gui.GuiMain;

public class Main {
    public static void main(String[] args) {
        Options options = new Options(args);
        options.parsePrams();
        Logger.INSTANCE = options.verbose ? new VerboseLogger(System.err) : new NormalLogger(System.err);

        printOptionWarnings(options);
        detectModeIfRequired(options);

        switch (options.mode) {
            case GUI:
                new GuiMain().start(options);
                return;
            case CUI:
                new CuiMain().start(options);
                return;
            default:
                throw new AssertionError();
        }
    }

    private static void printOptionWarnings(Options options) {
        for (String warning : options.warnings) {
            Logger.INSTANCE.warn(warning);
        }
    }

    private static void detectModeIfRequired(Options options) {
        if (options.mode == null) {
            Logger.INSTANCE.trace("mode is not specified. detecting...");
            if (options.anyValueParams()) {
                Logger.INSTANCE.trace("some params are specified, this means CUI");
                options.mode = LaunchMode.CUI;
            } else if (java.awt.GraphicsEnvironment.isHeadless()) {
                Logger.INSTANCE.trace("it's headless environment, this means CUI");
                options.mode = LaunchMode.CUI;
            } else {
                Logger.INSTANCE.trace("no params, this means GUI");
                options.mode = LaunchMode.GUI;
            }
        }
    }
}
