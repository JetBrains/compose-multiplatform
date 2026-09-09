/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CSSVariablesJvmTest {
    @OptIn(ComposeWebInternalApi::class)
    @Test
    fun gridAutoFlowAcceptsVariableReferences() {
        val style = StyleScopeBuilder()
        style.gridAutoFlow(CSSStyleVariable<GridAutoFlow>("flow").value())

        val declaration = style.properties.single()
        assertEquals("grid-auto-flow", declaration.name)
        assertEquals("var(--flow)", declaration.value.toString())
    }

    @Test
    fun variableReferencesUseTheJvmCarrier() {
        val stringValue = StylePropertyValue("text")
        val numericReference: CSSNumeric = CSSStyleVariable<CSSUnitValue>("spacing").value()

        assertFalse(stringValue is CSSNumericValue<*>)
        assertFalse(numericReference is CSSSizeValue<*>)
    }
}
