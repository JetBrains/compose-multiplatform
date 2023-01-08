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
internal const val MIN_JAVA_RUNTIME_VERSION = 15

@CacheableTask
abstract class AbstractCheckNativeDistributionRuntime : AbstractComposeDesktopTask() {
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    val javaHome: Property<String> = objects.notNullProperty()

    private val taskDir = project.layout.buildDirectory.dir("compose/tmp/$name")

    @get:OutputFile
    val javaRuntimePropertiesFile: Provider<RegularFile> = taskDir.map { it.file("properties.bin") }

    @get:LocalState
    val workingDir: Provider<Directory> = taskDir.map { it.dir("localState") }

    private val javaExec: File
        get() = getTool("java")

    private val javacExec: File
        get() = getTool("javac")

    private fun getTool(toolName: String): File {
        val javaHomeBin = File(javaHome.get()).resolve("bin")
        val tool = javaHomeBin.resolve(executableName(toolName))
        check(tool.exists()) { "Could not find $tool at: ${tool.absolutePath}}" }
        return tool
    }

    @TaskAction
    fun run() {
        taskDir.ioFile.mkdirs()

        val javaRuntimeVersion = try {
            getJavaRuntimeVersionUnsafe()?.toIntOrNull() ?: -1
        } catch (e: Exception) {
            throw IllegalStateException(
                "Could not infer Java runtime version for Java home directory: ${javaHome.get()}", e
            )
        }

        check(javaRuntimeVersion >= MIN_JAVA_RUNTIME_VERSION) {
            """|Packaging native distributions requires JDK runtime version >= $MIN_JAVA_RUNTIME_VERSION
               |Actual version: '${javaRuntimeVersion ?: "<unknown>"}'
               |Java home: ${javaHome.get()}
            """.trimMargin()
        }

        val modules = arrayListOf<String>()
        runExternalTool(
            tool = javaExec,
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

        val properties = JvmRuntimeProperties(javaRuntimeVersion, modules)
        JvmRuntimeProperties.writeToFile(properties, javaRuntimePropertiesFile.ioFile)
    }

    private fun getJavaRuntimeVersionUnsafe(): String? {
        fileOperations.clearDirs(workingDir)
        val workingDir = workingDir.ioFile

        val printJavaRuntimeClassName = "PrintJavaRuntimeVersion"
        val javaVersionPrefix = "Java runtime version = '"
        val javaVersionSuffix = "'"
        val printJavaRuntimeJava = workingDir.resolve("java/$printJavaRuntimeClassName.java").apply {
            parentFile.mkdirs()
            writeText("""
                import java.lang.reflect.Method;

                public class $printJavaRuntimeClassName {
                    public static void main(String[] args) {
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
                            printVersionAndHalt(System.getProperty("java.version"));
                        }
                    }

                    private static void printVersionAndHalt(String version) {
                        System.out.println("$javaVersionPrefix" + version + "$javaVersionSuffix");
                        Runtime.getRuntime().exit(0);
                    }
                }
            """.trimIndent())
        }
        val classFilesDir = workingDir.resolve("out-classes")
        runExternalTool(
            tool = javacExec,
            args = listOf(
                "-source", "1.8",
                "-target", "1.8",
                "-d", classFilesDir.absolutePath,
                printJavaRuntimeJava.absolutePath
            )
        )

        var javaRuntimeVersion: String? = null
        runExternalTool(
            tool = javaExec,
            args = listOf("-cp", classFilesDir.absolutePath, printJavaRuntimeClassName),
            processStdout = { stdout ->
                val m = "$javaVersionPrefix(.+)$javaVersionSuffix".toRegex().find(stdout)
                javaRuntimeVersion = m?.groupValues?.get(1)
            }
        )
        return javaRuntimeVersion
    }
}
