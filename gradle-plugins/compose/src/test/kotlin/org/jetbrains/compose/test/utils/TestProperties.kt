/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

object TestProperties {
    val composeCompilerVersion: String
        get() = notNullSystemProperty("compose.tests.compiler.version")

    val composeCompilerCompatibleKotlinVersion: String
        get() = notNullSystemProperty("compose.tests.compiler.compatible.kotlin.version")

    val composeJsCompilerCompatibleKotlinVersion: String
        get() = notNullSystemProperty("compose.tests.js.compiler.compatible.kotlin.version")

    val androidxCompilerVersion: String
        get() = notNullSystemProperty("compose.tests.androidx.compiler.version")

    val androidxCompilerCompatibleKotlinVersion: String
        get() = notNullSystemProperty("compose.tests.androidx.compiler.compatible.kotlin.version")

    val composeGradlePluginVersion: String
        get() = notNullSystemProperty("compose.tests.compose.gradle.plugin.version")

    val gradleVersionForTests: String?
        get() = System.getProperty("compose.tests.gradle.version")

    private fun notNullSystemProperty(property: String): String =
        System.getProperty(property) ?: error("The '$property' system property is not set")
}
