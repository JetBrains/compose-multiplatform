/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.css.keywords.CSSAutoKeyword
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CSSVariablesJvmTest {
    @Test
    fun builtInVariableReferencesUseTheJvmCarrier() {
        val stringValue = StylePropertyValue("text")
        val stringReference: StylePropertyString = CSSStyleVariable<StylePropertyString>("label").value()
        val numberReference: StylePropertyNumber = CSSStyleVariable<StylePropertyNumber>("order").value()
        val styleReference: CSSStyleValue = CSSStyleVariable<CSSStyleValue>("shadow").value()
        val numericReference: CSSNumeric = CSSStyleVariable<CSSUnitValue>("spacing").value()
        val enumReference: DisplayStyle = CSSStyleVariable<DisplayStyle>("layout").value()
        val keywordReference: CSSAutoKeyword = CSSStyleVariable<CSSAutoKeyword>("inset").value()

        assertFalse(stringValue is CSSNumericValue<*>)
        assertFalse(numericReference is CSSSizeValue<*>)
        assertEquals("var(--label)", stringReference.toString())
        assertEquals("var(--order)", numberReference.toString())
        assertEquals("var(--shadow)", styleReference.toString())
        assertEquals("var(--spacing)", numericReference.toString())
        assertEquals("var(--layout)", enumReference.toString())
        assertEquals("var(--inset)", keywordReference.toString())
    }
}
