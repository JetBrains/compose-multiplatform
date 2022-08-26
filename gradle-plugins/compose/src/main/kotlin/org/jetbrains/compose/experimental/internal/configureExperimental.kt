/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.internal

import org.gradle.api.Project
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.experimental.dsl.ExperimentalExtension
import org.jetbrains.compose.experimental.uikit.internal.configureExperimentalUikitApplication
import org.jetbrains.compose.experimental.web.internal.configureExperimentalWebApplication
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureExperimental(
    composeExt: ComposeExtension,
    experimentalExt: ExperimentalExtension
) {
    if (experimentalExt.uikit._isApplicationInitialized) {
        val mppExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            ?: error("'org.jetbrains.kotlin.multiplatform' plugin is required for using 'compose.experimental.uikit {}'")
        configureExperimentalUikitApplication(mppExt, experimentalExt.uikit.application)
    }

    if (experimentalExt.web._isApplicationInitialized) {
        val webExt = composeExt.extensions.getByType(WebExtension::class.java)
        for (target in webExt.targetsToConfigure(project)) {
            target.configureExperimentalWebApplication(experimentalExt.web.application)
        }
    }
}
