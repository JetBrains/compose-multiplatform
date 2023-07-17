/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal.utils

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

private const val COCOAPODS_PLUGIN_ID = "org.jetbrains.kotlin.native.cocoapods"
internal fun Project.withCocoapodsPlugin(fn: () -> Unit) {
    project.plugins.withId(COCOAPODS_PLUGIN_ID) {
        fn()
    }
}

internal val KotlinMultiplatformExtension.cocoapodsExt: CocoapodsExtension
    get() {
        val extensionAware = (this as? ExtensionAware) ?: error("KotlinMultiplatformExtension is not ExtensionAware")
        val extName = "cocoapods"
        val ext = extensionAware.extensions.findByName(extName) ?: error("KotlinMultiplatformExtension does not contain '$extName' extension")
        return ext as CocoapodsExtension
    }
