/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.dsl.DEFAULT_RUNTIME_MODULES
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.normalizedPath
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask

abstract class AbstractSuggestModulesTask : AbstractComposeDesktopTask() {
    @get:Input
    val javaHome: Property<String> = objects.notNullProperty<String>().apply {
        set(providers.systemProperty("java.home"))
    }

    @get:InputFiles
    val files: ConfigurableFileCollection = objects.fileCollection()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val launcherMainJar: RegularFileProperty = objects.fileProperty()

    @get:Input
    val modules: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Input
    val jvmTarget: Property<String> = objects.notNullProperty(MIN_JAVA_RUNTIME_VERSION.toString())

    @get:LocalState
    protected val workingDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/$name")

    @TaskAction
    fun run() {
        val jtool = jvmToolFile("jdeps", javaHome = javaHome)

        cleanDirs(workingDir)
        val args = arrayListOf<String>().apply {
            add("--print-module-deps")
            add("--ignore-missing-deps")
            add("--multi-release")
            add(jvmTarget.get())
            add("--class-path")
            add(files.joinToString(java.io.File.pathSeparator) { it.normalizedPath() })
            add(launcherMainJar.ioFile.normalizedPath())
        }

        try {
            runExternalTool(
                tool = jtool,
                args = args,
                forceLogToFile = true,
                processStdout = { output ->
                    val defaultModules = hashSetOf(*DEFAULT_RUNTIME_MODULES)
                    val suggestedModules = output.splitToSequence(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it !in defaultModules }
                        .toSortedSet()
                    val suggestion = "modules(${suggestedModules.joinToString(", ") { "\"$it\"" }})"
                    logger.quiet("Suggested runtime modules to include:")
                    logger.quiet(suggestion)
                }
            )
        } finally {
            if (!ComposeProperties.preserveWorkingDir(providers).get()) {
                fileOperations.delete(workingDir)
            }
        }
    }
}
