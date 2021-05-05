package com.anatawa12.jarInJar.creator.gui;

import com.anatawa12.jarInJar.creator.Logger;
import com.anatawa12.jarInJar.creator.commandline.Options;
import com.anatawa12.jarInJar.creator.commandline.VerboseLogger;

import java.awt.*;

public class GuiMain {
    public void start(Options options) {
        if (GraphicsEnvironment.isHeadless()) {
            Logger.INSTANCE.error("its' headless environment. Can't launch gui mode.");
            System.exit(-1);
        }

        Logger.INSTANCE = new VerboseLogger(System.err);
        Logger.INSTANCE.trace("for GUI, always use verbose.");

        Logger.INSTANCE.trace("Starting up GUI");
        MainFrame frame = new MainFrame();

        Toolkit it = Toolkit.getDefaultToolkit();
        Dimension d = it.getScreenSize();
        frame.setLocation(d.width / 2 - frame.getWidth() / 2, d.height / 2 - frame.getHeight() / 2);

        frame.setVisible(true);
    }
}
