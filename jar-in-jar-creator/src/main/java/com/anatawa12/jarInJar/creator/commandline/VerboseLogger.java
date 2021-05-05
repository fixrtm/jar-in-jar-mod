package com.anatawa12.jarInJar.creator.commandline;

import com.anatawa12.jarInJar.creator.Logger;

import java.io.PrintStream;

public class VerboseLogger extends Logger {
    private final PrintStream writeTo;

    public VerboseLogger(PrintStream writeTo) {
        super();
        this.writeTo = writeTo;
    }

    @Override
    public void error(String message) {
        writeTo.println("[ERROR]:" + message);
    }

    @Override
    public void warn(String message) {
        writeTo.println("[WARNING]:" + message);
    }

    @Override
    public void trace(String message) {
        writeTo.println("[TRACE]:" + message);
    }
}
