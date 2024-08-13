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
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.JvmRuntimeProperties
import org.jetbrains.compose.desktop.application.internal.ExternalToolRunner
import org.jetbrains.compose.desktop.application.internal.JdkVersionProbe
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.executableName
import org.jetbrains.compose.internal.utils.ioFile
import org.jetbrains.compose.internal.utils.notNullProperty
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*

// __COMPOSE_NATIVE_DISTRIBUTIONS_MIN_JAVA_VERSION__
internal const val MIN_JAVA_RUNTIME_VERSION = 17

@CacheableTask
abstract class AbstractCheckNativeDistributionRuntime : AbstractComposeDesktopTask() {
    @get:Classpath
    val jdkVersionProbeJar: ConfigurableFileCollection = objects.fileCollection()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    val jdkHome: Property<String> = objects.notNullProperty()

    @get:Input
    abstract val checkJdkVendor: Property<Boolean>

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

        val jdkRuntimeProperties = getJDKRuntimeProperties(javaExecutable)

        val jdkMajorVersionString = jdkRuntimeProperties.getProperty(JdkVersionProbe.JDK_MAJOR_VERSION_KEY)
        val jdkMajorVersion = jdkMajorVersionString?.toIntOrNull()
            ?: jdkDistributionProbingError("JDK version '$jdkMajorVersionString' has unexpected format")

        check(jdkMajorVersion >= MIN_JAVA_RUNTIME_VERSION) {
            jdkDistributionProbingError(
                "minimum required JDK version is '$MIN_JAVA_RUNTIME_VERSION', " +
                "but actual version is '$jdkMajorVersion'"
            )
        }

        if (checkJdkVendor.get()) {
            val vendor = jdkRuntimeProperties.getProperty(JdkVersionProbe.JDK_VENDOR_KEY)
            if (vendor == null) {
                logger.warn("JDK vendor probe failed: $jdkHome")
            } else {
                if (currentOS == OS.MacOS && vendor.equals("homebrew", ignoreCase = true)) {
                    error(
                        """
                            |Homebrew's JDK distribution may cause issues with packaging.
                            |See: https://github.com/JetBrains/compose-multiplatform/issues/3107
                            |Possible solutions:
                            |* Use other vendor's JDK distribution, such as Amazon Corretto;
                            |* To continue using Homebrew distribution for packaging on your own risk, add "${ComposeProperties.CHECK_JDK_VENDOR}=false" to your gradle.properties
                        """.trimMargin())
                }
            }
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

        val properties = JvmRuntimeProperties(jdkMajorVersion, modules)
        JvmRuntimeProperties.writeToFile(properties, javaRuntimePropertiesFile.ioFile)
    }

    private fun getJDKRuntimeProperties(javaExecutable: File): Properties {
        val jdkProperties = Properties()
        runExternalTool(
            tool = javaExecutable,
            args = listOf("-jar", jdkVersionProbeJar.files.single().absolutePath),
            processStdout = { stdout ->
                ByteArrayInputStream(stdout.trim().toByteArray()).use {
                    jdkProperties.loadFromXML(it)
                }
            }
        )
        return jdkProperties
    }
}
