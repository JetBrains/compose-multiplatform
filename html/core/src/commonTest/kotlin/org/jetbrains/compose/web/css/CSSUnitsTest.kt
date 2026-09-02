/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CSSUnitsTest {
    @Test
    fun unitTokensKeepTheirTextAndRuntimeType() {
        val px: CSSUnit = CSSUnit.px
        val percent: CSSUnit = CSSUnit.percent

        assertEquals("px", px.toString())
        assertEquals("%", percent.toString())
        assertTrue(px is CSSUnit.px)
        assertFalse(px is CSSUnit.percent)
        assertTrue(percent is CSSUnit.percent)
        assertFalse(percent is CSSUnit.px)
        assertSame(CSSUnit.px, CSSUnit.px)
        assertSame(CSSUnit.percent, CSSUnit.percent)
    }

    @Test
    fun typedUnitValuesUsePortableCssText() {
        assertEquals("4px", 4.px.toString())
        assertEquals("4.5px", 4.5.px.toString())
        assertEquals("0%", 0.percent.toString())
        assertEquals("0px", (-0.0).px.toString())
        assertEquals("250ms", 250.ms.toString())
        assertEquals("33.333332%", 33.333333.percent.toString())
        assertEquals("0.0001px", 0.0001.px.toString())
        assertEquals("1e-7px", 0.0000001.px.toString())
        assertEquals("0.5%", 0.5.percent.toString())
        assertEquals("250.5ms", 250.5.ms.toString())
    }

    @Test
    fun typedUnitValuesKeepValueAndUnitIdentity() {
        val value = 12.px

        assertEquals(12f, value.value)
        assertSame(CSSUnit.px, value.unit)
        assertEquals(12.px, value)
    }
}
