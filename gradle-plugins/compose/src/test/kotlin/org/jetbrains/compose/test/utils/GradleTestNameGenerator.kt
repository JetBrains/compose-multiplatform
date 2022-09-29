/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.junit.jupiter.api.DisplayNameGenerator

class GradleTestNameGenerator : DisplayNameGenerator.Standard() {
    private val gradleVersion = TestProperties.gradleVersionForTests?.let { "[Gradle '$it']" } ?: ""

    override fun generateDisplayNameForClass(testClass: Class<*>?): String =
        super.generateDisplayNameForClass(testClass) + gradleVersion
}