/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.files.mangledName
import org.jetbrains.compose.desktop.application.internal.files.normalizedPath
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.*
import java.io.File
import java.io.Writer
import kotlin.collections.LinkedHashMap

abstract class AbstractProguardTask : AbstractComposeDesktopTask() {
    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:InputFile
    val mainJar: RegularFileProperty = objects.fileProperty()

    @get:Internal
    internal val mainJarInDestinationDir: Provider<RegularFile> = mainJar.flatMap {
        destinationDir.file(it.asFile.name)
    }

    @get:InputFiles
    val configurationFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:Optional
    @get:Input
    val dontobfuscate: Property<Boolean?> = objects.nullableProperty()

    @get:Optional
    @get:Input
    val dontoptimize: Property<Boolean?> = objects.nullableProperty()

    @get:Optional
    @get:Input
    val joinOutputJars: Property<Boolean?> = objects.nullableProperty()

    // todo: DSL for excluding default rules
    // also consider pulling coroutines rules from coroutines artifact
    // https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro
    @get:Optional
    @get:InputFile
    val defaultComposeRulesFile: RegularFileProperty = objects.fileProperty()

    @get:Input
    val proguardVersion: Property<String> = objects.notNullProperty()

    @get:InputFiles
    val proguardFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:Input
    val javaHome: Property<String> = objects.notNullProperty(System.getProperty("java.home"))

    @get:Input
    val mainClass: Property<String> = objects.notNullProperty()

    @get:Internal
    val maxHeapSize: Property<String?> = objects.nullableProperty()

    @get:OutputDirectory
    val destinationDir: DirectoryProperty = objects.directoryProperty()

    @get:LocalState
    protected val workingDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/$name")

    private val rootConfigurationFile = workingDir.map { it.file("root-config.pro") }

    private val jarsConfigurationFile = workingDir.map { it.file("jars-config.pro") }

    @TaskAction
    fun execute() {
        val javaHome = File(javaHome.get())

        fileOperations.clearDirs(destinationDir, workingDir)
        val destinationDir = destinationDir.ioFile.absoluteFile

        // todo: can be cached for a jdk
        val jmods = javaHome.resolve("jmods").walk().filter {
            it.isFile && it.path.endsWith("jmod", ignoreCase = true)
        }.toList()

        val inputToOutputJars = LinkedHashMap<File, File>()
        // avoid mangling mainJar
        inputToOutputJars[mainJar.ioFile] = mainJarInDestinationDir.ioFile
        for (inputFile in inputFiles) {
            if (inputFile.name.endsWith(".jar", ignoreCase = true)) {
                inputToOutputJars.putIfAbsent(inputFile, destinationDir.resolve(inputFile.mangledName()))
            } else {
                inputFile.copyTo(destinationDir.resolve(inputFile.name))
            }
        }

        jarsConfigurationFile.ioFile.bufferedWriter().use { writer ->
            val toSingleOutputJar = joinOutputJars.orNull == true
            for ((input, output) in inputToOutputJars.entries) {
                writer.writeLn("-injars '${input.normalizedPath()}'")
                if (!toSingleOutputJar)
                    writer.writeLn("-outjars '${output.normalizedPath()}'")
            }
            if (toSingleOutputJar)
                writer.writeLn("-outjars '${mainJarInDestinationDir.ioFile.normalizedPath()}'")

            for (jmod in jmods) {
                writer.writeLn("-libraryjars '${jmod.normalizedPath()}'(!**.jar;!module-info.class)")
            }
        }

        rootConfigurationFile.ioFile.bufferedWriter().use { writer ->
            if (dontobfuscate.orNull == true) {
                writer.writeLn("-dontobfuscate")
            }

            if (dontoptimize.orNull == true) {
                writer.writeLn("-dontoptimize")
            }

            writer.writeLn("""
                -keep public class ${mainClass.get()} {
                    public static void main(java.lang.String[]);
                }
            """.trimIndent())

            val includeFiles = sequenceOf(
                jarsConfigurationFile.ioFile,
                defaultComposeRulesFile.ioFile
            ) + configurationFiles.files.asSequence()
            for (configFile in includeFiles.filterNotNull()) {
                writer.writeLn("-include '${configFile.normalizedPath()}'")
            }
        }

        val javaBinary = jvmToolFile(toolName = "java", javaHome = javaHome)
        val args = arrayListOf<String>().apply {
            val maxHeapSize = maxHeapSize.orNull
            if (maxHeapSize != null) {
                add("-Xmx:$maxHeapSize")
            }
            cliArg("-cp", proguardFiles.map { it.normalizedPath() }.joinToString(File.pathSeparator))
            add("proguard.ProGuard")
            // todo: consider separate flag
            cliArg("-verbose", verbose)
            cliArg("-include", rootConfigurationFile)
        }

        runExternalTool(
            tool = javaBinary,
            args = args,
            environment = emptyMap(),
            logToConsole = ExternalToolRunner.LogToConsole.Always
        ).assertNormalExitValue()
    }

    private fun Writer.writeLn(s: String) {
        write(s)
        write("\n")
    }
}