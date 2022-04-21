/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test

object TestProperties {
    val defaultKotlinVersion: String
        get() = notNullSystemProperty("kotlin.version")

    val composeVersion: String
        get() = notNullSystemProperty("compose.plugin.version")

    val gradleVersionForTests: String?
        get() = System.getProperty("gradle.version.for.tests")

    private fun notNullSystemProperty(property: String): String =
        System.getProperty(property) ?: error("The '$property' system property is not set")
}
