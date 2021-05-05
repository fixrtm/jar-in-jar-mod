package com.anatawa12.jarInJar.creator.commandline;

import com.anatawa12.jarInJar.creator.EmbedJarInJar;
import com.anatawa12.jarInJar.creator.Logger;
import com.anatawa12.jarInJar.creator.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CuiMain {
    boolean errored;

    public void start(Options options) {
        File realOut = null;
        // if input and output are same file
        if (!options.inputFile.equals("-")
                && new File(options.inputFile).getAbsoluteFile().equals(new File(options.outputFile).getAbsoluteFile())) {
            try {
                File outTemp = File.createTempFile("jar-in-jar-creator", ".tmp");
                outTemp.deleteOnExit();
                realOut = new File(options.outputFile).getAbsoluteFile();
                options.outputFile = outTemp.getAbsolutePath();
                if (!realOut.getParentFile().exists() && !realOut.getParentFile().mkdirs()) {
                    Logger.INSTANCE.error("Parent directory cannot be Created: " + realOut);
                    errored = true;
                }
            } catch (IOException e) {
                Logger.INSTANCE.error("Unexpected io error preparing output temporary buffer");
                Logger.INSTANCE.trace(e.toString());
                errored = true;
            }
        }

        EmbedJarInJar embedJarInJar = new EmbedJarInJar();

        readInputFileOption(options, embedJarInJar);
        readOutputFileOption(options, embedJarInJar);
        embedJarInJar.keepFmlJsonCache = options.keepFmlJsonCache;
        embedJarInJar.basePackage = options.basePackage;
        embedJarInJar.target = options.target;
        if (embedJarInJar.target == null) {
            Logger.INSTANCE.error("--target not specified");
            errored = true;
        }
        embedJarInJar.listener = new LoggingListener();
        if (errored) System.exit(2);

        try {
            embedJarInJar.runTask();
        } catch (IOException e) {
            Logger.INSTANCE.error(e.getMessage());
            if (errored) System.exit(3);
        }

        if (realOut != null) {
            try (FileInputStream tempIn = new FileInputStream(options.outputFile);
                 FileOutputStream fileOut = new FileOutputStream(realOut)) {
                Utils.copyStream(tempIn, fileOut, false);
            } catch (IOException e) {
                Logger.INSTANCE.error(e.getMessage());
                if (errored) System.exit(3);
            }
        }
    }

    private void readInputFileOption(Options options, EmbedJarInJar embedJarInJar) {
        if (options.inputFile == null) {
            Logger.INSTANCE.error("--input not specified");
            errored = true;
        } else if (options.inputFile.equals("-")) {
            Logger.INSTANCE.trace("--input - means stdin");
            embedJarInJar.input = System.in;
        } else {
            try {
                embedJarInJar.input = new FileInputStream(options.inputFile);
            } catch (FileNotFoundException e) {
                Logger.INSTANCE.error("File Not Found: " + e.getMessage());
                errored = true;
            }
        }
    }

    private void readOutputFileOption(Options options, EmbedJarInJar embedJarInJar) {
        if (options.outputFile == null) {
            Logger.INSTANCE.error("--output not specified");
            errored = true;
        } else if (options.outputFile.equals("-")) {
            Logger.INSTANCE.trace("--output - means stdout");
            embedJarInJar.destination = System.out;
        } else {
            File file = new File(options.outputFile).getAbsoluteFile();
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                Logger.INSTANCE.error("Parent directory cannot be Created: " + file);
                errored = true;
                return;
            }
            try {
                embedJarInJar.destination = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Logger.INSTANCE.error("File Not Found: " + e.getMessage());
                errored = true;
            }
        }
    }
}
