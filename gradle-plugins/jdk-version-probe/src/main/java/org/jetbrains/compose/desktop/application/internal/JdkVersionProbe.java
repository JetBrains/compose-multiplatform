/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.desktop.application.internal;

import java.lang.reflect.Method;

public class JdkVersionProbe {
    public static void main(String[] args) {
        Class<Runtime> runtimeClass = Runtime.class;
        try {
            Method version = runtimeClass.getMethod("version");
            Object runtimeVer = version.invoke(runtimeClass);
            Class<?> runtimeVerClass = runtimeVer.getClass();
            try {
                int feature = (int) runtimeVerClass.getMethod("feature").invoke(runtimeVer);
                printVersionAndHalt((Integer.valueOf(feature)).toString());
            } catch (NoSuchMethodException e) {
                int major = (int) runtimeVerClass.getMethod("major").invoke(runtimeVer);
                printVersionAndHalt((Integer.valueOf(major)).toString());
            }
        } catch (Exception e) {
            String javaVersion = System.getProperty("java.version");
            String[] parts = javaVersion.split("\\.");
            if (parts.length > 2 && "1".equalsIgnoreCase(parts[0])) {
                printVersionAndHalt(parts[1]);
            } else {
                throw new IllegalStateException("Could not determine JDK version from string: '" + javaVersion + "'");
            }
        }
    }

    private static void printVersionAndHalt(String version) {
        System.out.println(version);
        Runtime.getRuntime().exit(0);
    }
}