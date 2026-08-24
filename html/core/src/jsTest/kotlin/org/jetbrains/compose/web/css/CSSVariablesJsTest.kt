/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.js.jsTypeOf
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSVariablesJsTest {
    @Test
    fun variableReferencesUseRawJsStrings() {
        val stringValue = StylePropertyValue("text")
        val numericReference: CSSNumeric = CSSStyleVariable<CSSUnitValue>("spacing").value()

        assertEquals("string", jsTypeOf(stringValue))
        assertEquals("string", jsTypeOf(numericReference))
    }
}
