/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.desktop.application.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

public class JdkVersionProbe {
    public final static String JDK_MAJOR_VERSION_KEY = "jdk.major.version";
    public final static String JDK_VENDOR_KEY = "jdk.vendor";

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(JDK_MAJOR_VERSION_KEY, getJDKMajorVersion());
        properties.setProperty(JDK_VENDOR_KEY, System.getProperty("java.vendor"));
        properties.storeToXML(System.out, null);
    }

    private static String getJDKMajorVersion() {
        Class<Runtime> runtimeClass = Runtime.class;
        try {
            Method version = runtimeClass.getMethod("version");
            Object runtimeVer = version.invoke(runtimeClass);
            Class<?> runtimeVerClass = runtimeVer.getClass();
            try {
                int feature = (int) runtimeVerClass.getMethod("feature").invoke(runtimeVer);
                return (Integer.valueOf(feature)).toString();
            } catch (NoSuchMethodException e) {
                int major = (int) runtimeVerClass.getMethod("major").invoke(runtimeVer);
                return (Integer.valueOf(major)).toString();
            }
        } catch (Exception e) {
            String javaVersion = System.getProperty("java.version");
            String[] parts = javaVersion.split("\\.");
            if (parts.length > 2 && "1".equalsIgnoreCase(parts[0])) {
                return parts[1];
            } else {
                throw new IllegalStateException("Could not determine JDK version from string: '" + javaVersion + "'");
            }
        }
    }
}