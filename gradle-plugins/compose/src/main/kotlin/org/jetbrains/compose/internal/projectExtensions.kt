/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.util.GradleVersion
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.composeExt: ComposeExtension?
    get() = extensions.findByType(ComposeExtension::class.java)

internal val Project.webExt: WebExtension?
    get() = composeExt?.extensions?.findByType(WebExtension::class.java)

internal val Project.mppExt: KotlinMultiplatformExtension
    get() = mppExtOrNull ?: error("Could not find KotlinMultiplatformExtension ($project)")

internal val Project.mppExtOrNull: KotlinMultiplatformExtension?
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

internal val Project.kotlinJsExtOrNull: KotlinJsProjectExtension?
    get() = extensions.findByType(KotlinJsProjectExtension::class.java)

internal val Project.javaSourceSets: SourceSetContainer
    get() = if (GradleVersion.current() < GradleVersion.version("7.1")) {
        convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    } else {
        extensions.getByType(JavaPluginExtension::class.java).sourceSets
    }
