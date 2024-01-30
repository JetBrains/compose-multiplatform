/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.service

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.compose.experimental.internal.SUPPORTED_NATIVE_CACHE_KIND_PROPERTIES
import org.jetbrains.compose.internal.utils.loadProperties
import org.jetbrains.compose.internal.utils.localPropertiesFile

internal abstract class GradlePropertySnapshotService : AbstractComposeMultiplatformBuildService<GradlePropertySnapshotService.Parameters>() {
    interface Parameters : BuildServiceParameters {
        val gradlePropertiesCacheKindSnapshot: MapProperty<String, String>
        val localPropertiesCacheKindSnapshot: MapProperty<String, String>
    }

    internal val gradleProperties: Map<String, String> = parameters.gradlePropertiesCacheKindSnapshot.get()
    internal val localProperties: Map<String, String> = parameters.localPropertiesCacheKindSnapshot.get()

    companion object {
        fun init(project: Project) {
            registerServiceIfAbsent<GradlePropertySnapshotService, Parameters>(project) {
                // WORKAROUND! Call getter at least once, because of Issue: https://github.com/gradle/gradle/issues/27099
                gradlePropertiesCacheKindSnapshot
                localPropertiesCacheKindSnapshot
                initParams(project)
            }
        }

        fun getInstance(project: Project): GradlePropertySnapshotService =
            getExistingServiceRegistration<GradlePropertySnapshotService, Parameters>(project).service.get()

        private fun Parameters.initParams(project: Project) {
            // we want to record original properties (explicitly set by a user)
            // before we possibly change them in configureNativeCompilerCaching.kt
            val rootProject = project.rootProject
            val localProperties = loadProperties(rootProject.localPropertiesFile)
            for (cacheKindProperty in SUPPORTED_NATIVE_CACHE_KIND_PROPERTIES) {
                rootProject.findProperty(cacheKindProperty)?.toString()?.let { value ->
                    gradlePropertiesCacheKindSnapshot.put(cacheKindProperty, value)
                }
                localProperties[cacheKindProperty]?.toString()?.let { value ->
                    localPropertiesCacheKindSnapshot.put(cacheKindProperty, value)
                }
            }
        }
    }
}