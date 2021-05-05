package com.anatawa12.jarInJar;

public abstract class Logger {
    public static Logger getLogger(String s) {
        try {
            Class.forName("org.apache.logging.log4j.LogManager");
            return new Log4JLogger(s);
        } catch (ClassNotFoundException ignored) {
            return new JavaUtilLogger(s);
        }
    }

    public abstract void error(String s, Object param);
    public abstract void warn(String s, Object param);
    public abstract void info(String s);
    public abstract void debug(String s, Object param);

    private static class Log4JLogger extends Logger {
        private org.apache.logging.log4j.Logger LOGGER;

        public Log4JLogger(String s) {
            LOGGER = org.apache.logging.log4j.LogManager.getLogger(s);
        }

        @Override
        public void error(String s, Object param) {
            LOGGER.log(org.apache.logging.log4j.Level.ERROR, s, param);
        }

        @Override
        public void warn(String s, Object param) {
            LOGGER.log(org.apache.logging.log4j.Level.WARN, s, param);
        }

        @Override
        public void info(String s) {
            LOGGER.log(org.apache.logging.log4j.Level.INFO, s);
        }

        @Override
        public void debug(String s, Object param) {
            LOGGER.log(org.apache.logging.log4j.Level.DEBUG, s, param);
        }
    }

    private static class JavaUtilLogger extends Logger {
        private java.util.logging.Logger LOGGER;

        public JavaUtilLogger(String s) {
            LOGGER = java.util.logging.LogManager.getLogManager().getLogger(s);
        }

        @Override
        public void error(String s, Object param) {
            LOGGER.log(java.util.logging.Level.SEVERE, s, param);
        }

        @Override
        public void warn(String s, Object param) {
            LOGGER.log(java.util.logging.Level.WARNING, s, param);
        }

        @Override
        public void info(String s) {
            LOGGER.log(java.util.logging.Level.INFO, s);
        }

        @Override
        public void debug(String s, Object param) {
            LOGGER.log(java.util.logging.Level.FINE, s, param);
        }
    }
}
