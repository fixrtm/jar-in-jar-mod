package com.anatawa12.jarInJar.creator.gui;

import com.anatawa12.jarInJar.creator.Logger;
import com.anatawa12.jarInJar.creator.commandline.Options;

import java.awt.*;

public class GuiMain {
    public void start(Options options) {
        if (GraphicsEnvironment.isHeadless()) {
            Logger.INSTANCE.error("its' headless environment. Can't launch gui mode.");
            System.exit(-1);
        }
        Logger.INSTANCE.error("Gui is not supported yet");
        System.exit(-1);
    }
}
