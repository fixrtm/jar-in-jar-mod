package com.anatawa12.jarInJar.creator.commandline;

import com.anatawa12.jarInJar.creator.EmbedProgressListener;
import com.anatawa12.jarInJar.creator.Logger;

public class LoggingListener implements EmbedProgressListener {
    private int indent = 0;

    private String message(String message) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < indent; i++)
            builder.append('+');
        builder.append(message);
        return builder.toString();
    }

    @Override
    public void begin(String name) {
        Logger.INSTANCE.trace(message("begin:" + name));
        indent++;
    }

    @Override
    public void end() {
        indent--;
        Logger.INSTANCE.trace(message("end"));
    }

    @Override
    public void then(String name) {
        Logger.INSTANCE.trace(message("begin:" + name));
    }
}
