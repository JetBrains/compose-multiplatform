/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class ComposeCompilerKotlinSupportPlugin : KotlinCompilerPluginSupportPlugin {
    override fun getCompilerPluginId(): String =
        "org.jetbrains.compose"

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val targetPlatform = kotlinCompilation.target.platformType
        return targetPlatform != KotlinPlatformType.js
                && targetPlatform != KotlinPlatformType.native
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "org.jetbrains.compose.compiler", artifactId = "compiler", version = composeVersion
        )
}