/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.ConfigurableFileCollection
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
import java.io.File

// __COMPOSE_NATIVE_DISTRIBUTIONS_MIN_JAVA_VERSION__
internal const val MIN_JAVA_RUNTIME_VERSION = 17

@CacheableTask
abstract class AbstractCheckNativeDistributionRuntime : AbstractComposeDesktopTask() {
    @get:Classpath
    val jdkVersionProbeJar: ConfigurableFileCollection = objects.fileCollection()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    val jdkHome: Property<String> = objects.notNullProperty()

    private val taskDir = project.layout.buildDirectory.dir("compose/tmp/$name")

    @get:OutputFile
    val javaRuntimePropertiesFile: Provider<RegularFile> = taskDir.map { it.file("properties.bin") }

    private val jdkHomeFile: File
        get() = File(jdkHome.orNull ?: error("Missing jdkHome value"))

    private fun File.getJdkTool(toolName: String): File =
        resolve("bin/${executableName(toolName)}")

    private fun ensureToolsExist(vararg tools: File) {
        val missingTools = tools.filter { !it.exists() }.map { "'${it.name}'" }

        if (missingTools.isEmpty()) return

        if (missingTools.size == 1) jdkDistributionProbingError("${missingTools.single()} is missing")

        jdkDistributionProbingError("${missingTools.joinToString(", ")} are missing")
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
        val jlinkExecutable = jdkHome.getJdkTool("jlink")
        val jpackageExecutabke = jdkHome.getJdkTool("jpackage")
        ensureToolsExist(javaExecutable, jlinkExecutable, jpackageExecutabke)

        val jvmRuntimeVersionString = getJavaRuntimeVersion(javaExecutable)

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

    private fun getJavaRuntimeVersion(javaExecutable: File): String? {
        var javaRuntimeVersion: String? = null
        runExternalTool(
            tool = javaExecutable,
            args = listOf("-jar", jdkVersionProbeJar.files.single().absolutePath),
            processStdout = { stdout -> javaRuntimeVersion = stdout.trim() }
        )
        return javaRuntimeVersion
    }
}
