/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.jetbrains.compose.ComposeBuildConfig
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

internal fun Project.detachedComposeGradleDependency(
    artifactId: String,
    groupId: String = "org.jetbrains.compose",
): Configuration =
    detachedDependency(groupId = groupId, artifactId = artifactId, version = ComposeBuildConfig.composeGradlePluginVersion)

internal fun Project.detachedComposeDependency(
    artifactId: String,
    groupId: String = "org.jetbrains.compose",
): Configuration =
    detachedDependency(groupId = groupId, artifactId = artifactId, version = ComposeBuildConfig.composeVersion)

internal fun Project.detachedDependency(
    groupId: String,
    artifactId: String,
    version: String
): Configuration =
    project.configurations.detachedConfiguration(
        project.dependencies.create("$groupId:$artifactId:$version")
    )

internal fun Configuration.excludeTransitiveDependencies(): Configuration =
    apply { isTransitive = false }

internal inline fun <reified SubT> DomainObjectCollection<*>.configureEachWithType(
    crossinline fn: SubT.() -> Unit
) {
    configureEach {
        if (it is SubT) {
            it.fn()
        }
    }
}
