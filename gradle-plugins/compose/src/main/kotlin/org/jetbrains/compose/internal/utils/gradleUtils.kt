/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.util.*

internal inline fun Logger.info(fn: () -> String) {
    if (isInfoEnabled) {
        info(fn())
    }
}

internal inline fun Logger.debug(fn: () -> String) {
    if (isDebugEnabled) {
        debug(fn())
    }
}

val Project.localPropertiesFile get() = project.rootProject.file("local.properties")

fun Project.getLocalProperty(key: String): String? {
    if (localPropertiesFile.exists()) {
        val properties = Properties()
        localPropertiesFile.inputStream().buffered().use { input ->
            properties.load(input)
        }
        return properties.getProperty(key)
    } else {
        localPropertiesFile.createNewFile()
        return null
    }
}
