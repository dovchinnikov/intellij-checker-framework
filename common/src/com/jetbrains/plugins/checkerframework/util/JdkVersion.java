package com.jetbrains.plugins.checkerframework.util;

public class JdkVersion {

    public static final double JDK_VERSION = getJdkVersion();

    public static boolean check() {
        return JDK_VERSION >= 1.8;
    }

    public static double getJdkVersion() {
        final String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.6")) return 1.6;
        if (javaVersion.startsWith("1.7")) return 1.7;
        if (javaVersion.startsWith("1.8")) return 1.8;
        if (javaVersion.startsWith("1.9")) return 1.9;
        return 1.6;
    }

}
