/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.gradle.util.GradleVersion
import java.io.File

private const val COMPOSE_TESTS_GRADLE_VERSION_PROPERTY = "compose.tests.gradle.version"

object TestProperties {
    val composeCompilerVersion: String
        get() = notNullSystemProperty("compose.tests.compiler.version")

    val composeCompilerCompatibleKotlinVersion: String
        get() = notNullSystemProperty("compose.tests.compiler.compatible.kotlin.version")

    val composeJsCompilerCompatibleKotlinVersion: String
        get() = notNullSystemProperty("compose.tests.js.compiler.compatible.kotlin.version")

    val composeGradlePluginVersion: String
        get() = notNullSystemProperty("compose.tests.compose.gradle.plugin.version")

    val gradleVersionForTests: String
        get() = System.getProperty(COMPOSE_TESTS_GRADLE_VERSION_PROPERTY)
            ?: error("System property '$COMPOSE_TESTS_GRADLE_VERSION_PROPERTY' is not set")

    val gradleBaseVersionForTests: GradleVersion
        get() = GradleVersion.version(gradleVersionForTests).baseVersion

    val gradleConfigurationCache: Boolean
        get() = System.getProperty("compose.tests.gradle.configuration.cache") == "true"

    val summaryFile: File?
        get() = System.getProperty("compose.tests.summary.file")?.let { File(it) }

    val testJdksRoot: File?
        get() = System.getProperty("compose.tests.gradle.test.jdks.root")?.let { File(it) }

    private fun notNullSystemProperty(property: String): String =
        System.getProperty(property) ?: error("The '$property' system property is not set")
}
