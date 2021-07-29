/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.composeExt: ComposeExtension?
    get() = extensions.findByType(ComposeExtension::class.java)

internal val Project.webExt: WebExtension?
    get() = composeExt?.extensions?.findByType(WebExtension::class.java)

internal val Project.mppExt: KotlinMultiplatformExtension
    get() = mppExtOrNull ?: error("Could not find KotlinMultiplatformExtension ($project)")

internal val Project.mppExtOrNull: KotlinMultiplatformExtension?
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

internal val Project.javaExt: JavaPluginExtension
    get() = javaExtOrNull ?: error("Could not find JavaPluginExtension ($project)")

internal val Project.javaExtOrNull: JavaPluginExtension?
    get() = extensions.findByType(JavaPluginExtension::class.java)
