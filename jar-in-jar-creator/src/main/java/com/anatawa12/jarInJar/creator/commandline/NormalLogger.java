package com.anatawa12.jarInJar.creator.commandline;

import com.anatawa12.jarInJar.creator.Logger;

import java.io.PrintStream;

public class NormalLogger extends Logger {
    private final PrintStream writeTo;

    public NormalLogger(PrintStream writeTo) {
        super();
        this.writeTo = writeTo;
    }

    @Override
    public void error(String message) {
        writeTo.println("e:" + message);
    }

    @Override
    public void warn(String message) {
        writeTo.println("w:" + message);
    }

    @Override
    public void trace(String message) {
        // nop
    }
}
