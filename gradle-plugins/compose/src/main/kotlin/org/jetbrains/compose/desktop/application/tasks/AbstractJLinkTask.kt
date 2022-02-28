/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.RuntimeCompressionLevel
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.JavaRuntimeProperties
import org.jetbrains.compose.desktop.application.internal.cliArg
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import java.io.File

// todo: public DSL
// todo: deduplicate if multiple runtimes are created
abstract class AbstractJLinkTask : AbstractJvmToolOperationTask("jlink") {
    @get:Input
    val modules: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Input
    val includeAllModules: Property<Boolean> = objects.notNullProperty()

    @get:InputFile
    val javaRuntimePropertiesFile: RegularFileProperty = objects.fileProperty()

    @get:Input
    internal val stripDebug: Property<Boolean> = objects.notNullProperty(true)

    @get:Input
    internal val noHeaderFiles: Property<Boolean> = objects.notNullProperty(true)

    @get:Input
    internal val noManPages: Property<Boolean> = objects.notNullProperty(true)

    @get:Input
    internal val stripNativeCommands: Property<Boolean> = objects.notNullProperty(true)

    @get:Input
    @get:Optional
    internal val compressionLevel: Property<RuntimeCompressionLevel?> = objects.nullableProperty()

    override fun makeArgs(tmpDir: File): MutableList<String> = super.makeArgs(tmpDir).apply {
        val modulesToInclude =
            if (includeAllModules.get()) {
                JavaRuntimeProperties.readFromFile(javaRuntimePropertiesFile.ioFile).availableModules
            } else modules.get()
        modulesToInclude.forEach { m ->
            cliArg("--add-modules", m)
        }

        cliArg("--strip-debug", stripDebug)
        cliArg("--no-header-files", noHeaderFiles)
        cliArg("--no-man-pages", noManPages)
        cliArg("--strip-native-commands", stripNativeCommands)
        cliArg("--compress", compressionLevel.orNull?.id)

        cliArg("--output", destinationDir)
    }
}