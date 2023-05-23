/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.internal.JvmRuntimeProperties
import org.jetbrains.compose.internal.utils.executableName
import org.jetbrains.compose.internal.utils.ioFile
import org.jetbrains.compose.internal.utils.notNullProperty
import org.jetbrains.compose.desktop.application.internal.ExternalToolRunner
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.clearDirs
import java.io.File

// __COMPOSE_NATIVE_DISTRIBUTIONS_MIN_JAVA_VERSION__
internal const val MIN_JAVA_RUNTIME_VERSION = 17

@CacheableTask
abstract class AbstractCheckNativeDistributionRuntime : AbstractComposeDesktopTask() {
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    val jdkHome: Property<String> = objects.notNullProperty()

    private val taskDir = project.layout.buildDirectory.dir("compose/tmp/$name")

    @get:OutputFile
    val javaRuntimePropertiesFile: Provider<RegularFile> = taskDir.map { it.file("properties.bin") }

    @get:LocalState
    val workingDir: Provider<Directory> = taskDir.map { it.dir("localState") }

    private val jdkHomeFile: File
        get() = File(jdkHome.orNull ?: error("Missing jdkHome value"))

    private fun File.getJdkTool(toolName: String): File =
        resolve("bin/${executableName(toolName)}").apply {
            check(exists()) {
                jdkDistributionProbingError("'$toolName' is missing")
            }
        }

    private fun jdkDistributionProbingError(errorMessage: String): Nothing {
        val fullErrorMessage = buildString {
            appendLine("Failed to check JDK distribution: $errorMessage")
            appendLine("JDK distribution path: ${jdkHomeFile.absolutePath}")
        }
        error(fullErrorMessage)
    }

    @TaskAction
    fun run() {
        taskDir.ioFile.mkdirs()

        val jdkHome = jdkHomeFile
        val javaExecutable = jdkHome.getJdkTool("java")
        val javacExecutable = jdkHome.getJdkTool("javac")

        val jvmRuntimeVersionString = getJavaRuntimeVersion(
            javaExecutable = javaExecutable,
            javacExecutable = javacExecutable
        )

        val jvmRuntimeVersion = jvmRuntimeVersionString?.toIntOrNull()
            ?: jdkDistributionProbingError("JDK version '$jvmRuntimeVersionString' has unexpected format")

        check(jvmRuntimeVersion >= MIN_JAVA_RUNTIME_VERSION) {
            jdkDistributionProbingError(
                "minimum required JDK version is '$MIN_JAVA_RUNTIME_VERSION', " +
                "but actual version is '$jvmRuntimeVersion'"
            )
        }

        val modules = arrayListOf<String>()
        runExternalTool(
            tool = javaExecutable,
            args = listOf("--list-modules"),
            logToConsole = ExternalToolRunner.LogToConsole.Never,
            processStdout = { stdout ->
                stdout.lineSequence().forEach { line ->
                    val moduleName = line.trim().substringBefore("@")
                    if (moduleName.isNotBlank()) {
                        modules.add(moduleName)
                    }
                }
            }
        )

        val properties = JvmRuntimeProperties(jvmRuntimeVersion, modules)
        JvmRuntimeProperties.writeToFile(properties, javaRuntimePropertiesFile.ioFile)
    }

    private fun getJavaRuntimeVersion(
        javaExecutable: File,
        javacExecutable: File
    ): String? {
        fileOperations.clearDirs(workingDir)
        val workingDir = workingDir.ioFile

        val printJavaRuntimeClassName = "PrintJavaRuntimeVersion"
        val printJavaRuntimeJava = workingDir.resolve("java/$printJavaRuntimeClassName.java").apply {
            parentFile.mkdirs()
            writeText("""
                import java.lang.reflect.Method;

                public class $printJavaRuntimeClassName {
                    Class<Runtime> runtimeClass = Runtime.class;
                    try {
                        Method version = runtimeClass.getMethod("version");
                        Object runtimeVer = version.invoke(runtimeClass);
                        Class<? extends Object> runtimeVerClass = runtimeVer.getClass();
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

                    private static void printVersionAndHalt(String version) {
                        System.out.println(version);
                        Runtime.getRuntime().exit(0);
                    }
                }
            """.trimIndent())
        }
        val classFilesDir = workingDir.resolve("out-classes")
        runExternalTool(
            tool = javacExecutable,
            args = listOf(
                "-source", "1.8",
                "-target", "1.8",
                "-d", classFilesDir.absolutePath,
                printJavaRuntimeJava.absolutePath
            )
        )

        var javaRuntimeVersion: String? = null
        runExternalTool(
            tool = javaExecutable,
            args = listOf("-cp", classFilesDir.absolutePath, printJavaRuntimeClassName),
            processStdout = { stdout -> javaRuntimeVersion = stdout.trim() }
        )
        return javaRuntimeVersion
    }
}
