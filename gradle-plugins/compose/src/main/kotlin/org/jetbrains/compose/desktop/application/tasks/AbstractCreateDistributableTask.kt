/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.work.DisableCachingByDefault

/**
 * Creates the distributable (the app image) by depending on the tasks that actually do the work,
 * and exposes the resulting directory as its output.
 */
@DisableCachingByDefault(because = "Only exposes the output of the tasks it depends on")
abstract class AbstractCreateDistributableTask : DefaultTask() {
    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty
}
