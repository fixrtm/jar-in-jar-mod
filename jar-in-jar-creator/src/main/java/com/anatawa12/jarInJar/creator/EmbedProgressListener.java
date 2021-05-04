package com.anatawa12.jarInJar.creator;

public interface EmbedProgressListener {
    void begin(String name);
    void end();

    default void then(String name) {
        end();
        begin(name);
    }
}
