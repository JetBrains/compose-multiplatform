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
    fun typedUnitValuesUsePortableCssNumberFormatting() {
        assertEquals("4px", 4.px.toString())
        assertEquals("4.5px", 4.5.px.toString())
        assertEquals("0%", 0.percent.toString())
        assertEquals("0px", (-0.0).px.toString())
        assertEquals("250ms", 250.ms.toString())
    }

    @Test
    fun typedUnitValuesKeepValueAndUnitIdentity() {
        val value = 12.px

        assertEquals(12f, value.value)
        assertSame(CSSUnit.px, value.unit)
        assertEquals(12.px, value)
    }
}
