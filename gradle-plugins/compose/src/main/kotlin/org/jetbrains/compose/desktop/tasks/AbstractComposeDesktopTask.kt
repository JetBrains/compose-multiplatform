/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.ExternalToolRunner
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import javax.inject.Inject

abstract class AbstractComposeDesktopTask : DefaultTask() {
    @get:Inject
    protected abstract val objects: ObjectFactory

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val fileOperations: FileOperations

    @get:LocalState
    protected val logsDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/logs/$name")

    @get:Internal
    val verbose: Property<Boolean> = objects.notNullProperty<Boolean>().apply {
        set(providers.provider {
            logger.isDebugEnabled || ComposeProperties.isVerbose(providers).get()
        })
    }

    @get:Internal
    internal val runExternalTool: ExternalToolRunner
        get() = ExternalToolRunner(verbose, logsDir, execOperations)

    protected fun cleanDirs(vararg dirs: Provider<out FileSystemLocation>) {
        for (dir in dirs) {
            fileOperations.delete(dir)
            fileOperations.mkdir(dir)
        }
    }
}