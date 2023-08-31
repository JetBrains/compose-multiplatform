/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import org.gradle.api.Project
import org.jetbrains.compose.internal.service.GradlePropertySnapshotService

/**
 * Reads Kotlin Gradle plugin properties.
 *
 * Kotlin Gradle plugin supports reading property from two sources:
 * 1. Gradle properties. Normally located in gradle.properties file,
 * but can also be provided via command-line, <GRADLE_HOME>/gradle.properties
 * or can be set via Gradle API.
 * 2. local.properties file. local.properties file is not supported by Gradle out-of-the-box.
 * Nevertheless, it became a widespread convention.
 */
internal abstract class KGPPropertyProvider {
    abstract fun valueOrNull(propertyName: String): String?
    abstract val location: String

    class GradleProperties(private val project: Project) : KGPPropertyProvider() {
        override fun valueOrNull(propertyName: String): String? =
            GradlePropertySnapshotService.getInstance(project).gradleProperties[propertyName]
        override val location: String = "gradle.properties"
    }

    class LocalProperties(private val project: Project) : KGPPropertyProvider() {
        override fun valueOrNull(propertyName: String): String? =
            GradlePropertySnapshotService.getInstance(project).localProperties[propertyName]
        override val location: String = "local.properties"
    }
}