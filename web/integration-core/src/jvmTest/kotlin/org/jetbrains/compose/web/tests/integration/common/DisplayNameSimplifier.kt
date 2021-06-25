/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

class DisplayNameSimplifier : DisplayNameGenerator.Standard() {
    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method?): String {
        return super
            .generateDisplayNameForMethod(testClass, testMethod)
            .replace("WebDriver", "")
            .replace("()", "")
            .plus(" ")
    }
}
