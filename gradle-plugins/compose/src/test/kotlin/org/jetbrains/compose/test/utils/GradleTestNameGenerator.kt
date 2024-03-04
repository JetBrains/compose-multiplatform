/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

class GradleTestNameGenerator : DisplayNameGenerator.Standard() {

    override fun generateDisplayNameForMethod(testClass: Class<*>, testMethod: Method) =
        testMethod.name + with(TestProperties) {
            mutableListOf<String>().apply {
                muteException { add("kotlin=$composeCompilerCompatibleKotlinVersion") }
                muteException { add("gradle=$gradleVersion") }
                muteException { add("agp=$agpVersion") }
            }.joinToString(prefix = "(", separator = ", ", postfix = ")")
        }

    private fun muteException(fn: () -> Unit) = try {
        fn()
    } catch (_: Exception) {
        //do nothing
    }
}