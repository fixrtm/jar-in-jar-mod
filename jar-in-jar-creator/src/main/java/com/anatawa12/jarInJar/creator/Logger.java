package com.anatawa12.jarInJar.creator;

public abstract class Logger {
    public static Logger INSTANCE;

    public abstract void error(String message);
    public abstract void warn(String message);
    public abstract void trace(String message);

}
