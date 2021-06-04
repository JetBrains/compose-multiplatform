/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.css.CSS
import org.jetbrains.compose.web.css.CSSUnitValue
import org.jetbrains.compose.web.css.ch
import org.jetbrains.compose.web.css.cm
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.deg
import org.jetbrains.compose.web.css.dpcm
import org.jetbrains.compose.web.css.dpi
import org.jetbrains.compose.web.css.dppx
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.fr
import org.jetbrains.compose.web.css.grad
import org.jetbrains.compose.web.css.mm
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.number
import org.jetbrains.compose.web.css.pc
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.pt
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rad
import org.jetbrains.compose.web.css.s
import org.jetbrains.compose.web.css.turn
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vmax
import org.jetbrains.compose.web.css.vmin
import org.jetbrains.compose.web.css.vw
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSUnitApiTests {
    // TODO: Cover CSS.Q, CSS.khz and CSS.hz after we'll get rid from polyfill

    private fun CSSUnitValue.assertStructure(value: Number, unit: String, description: String? = null)  {
        assertEquals(this.value, value, description)
        assertEquals(this.unit, unit, description)
    }

    // TODO: use regular assertEqual after we'll get rid from polyfill
    private fun CSSUnitValue.assertStructure(otherUnit: CSSUnitValue, description: String? = null) {
        return assertStructure(otherUnit.value, otherUnit.unit, description)
    }

    @Test
    fun builderInvocation() {
        CSS.number(4).assertStructure(4, "number")
        CSS.percent(4).assertStructure(4, "percent")

        CSS.em(4).assertStructure(4, "em")
        CSS.ch(4).assertStructure(4, "ch")

        CSS.rem(4).assertStructure(4, "rem")

        CSS.vw(4).assertStructure(4, "vw")
        CSS.vh(4).assertStructure(4, "vh")

        CSS.vmin(4).assertStructure(4, "vmin")
        CSS.vmax(4).assertStructure(4, "vmax")
        CSS.cm(4).assertStructure(4, "cm")
        CSS.mm(4).assertStructure(4, "mm")

        CSS.pt(4).assertStructure(4, "pt")
        CSS.pc(4).assertStructure(4, "pc")
        CSS.px(4).assertStructure(4, "px")

        CSS.deg(4).assertStructure(4, "deg")
        CSS.grad(4).assertStructure(4, "grad")
        CSS.rad(4).assertStructure(4, "rad")
        CSS.turn(4).assertStructure(4, "turn")

        CSS.s(4).assertStructure(4, "s")
        CSS.ms(4).assertStructure(4, "ms")

        CSS.dpi(4).assertStructure(4, "dpi")
        CSS.dpcm(4).assertStructure(4, "dpcm")
        CSS.dppx(4).assertStructure(4, "dppx")

        CSS.fr(4).assertStructure(4, "fr")
    }

    @Test
    fun postfixInvocation() {
        4.number.assertStructure(CSS.number(4), "number postfix")
        4.percent.assertStructure(CSS.percent(4), "percent posfix")

        4.em.assertStructure(CSS.em(4), "em postfix")
        4.ch.assertStructure(CSS.ch(4), "ch postfix")

        4.cssRem.assertStructure(CSS.rem(4))

        4.vw.assertStructure(CSS.vw(4),"vw postfix")
        4.vh.assertStructure(CSS.vh(4), "vh postfix")

        4.vmin.assertStructure(CSS.vmin(4), "vmin postfix")
        4.vmax.assertStructure(CSS.vmax(4), "vmax postfix")
        4.cm.assertStructure(CSS.cm(4), "cm postfix")
        4.mm.assertStructure(CSS.mm(4), "mm postfix")

        4.pt.assertStructure(CSS.pt(4), "pt postfix")
        4.pc.assertStructure(CSS.pc(4), "pc postfix")
        4.px.assertStructure(CSS.px(4), "px postfix")

        4.deg.assertStructure(CSS.deg(4), "deg postfix")
        4.grad.assertStructure(CSS.grad(4), "grad postfix")
        4.rad.assertStructure(CSS.rad(4), "rad postfix")
        4.turn.assertStructure(CSS.turn(4), "turn postfix")

        4.s.assertStructure(CSS.s(4), "s postfix")
        4.ms.assertStructure(CSS.ms(4), "ms postfix")

        4.dpi.assertStructure(CSS.dpi(4), "dpi postfix")
        4.dpcm.assertStructure(CSS.dpcm(4), "dpcm postfix")
        4.dppx.assertStructure(CSS.dppx(4), "dppx postfix")

        4.fr.assertStructure(CSS.fr(4), "fr postfix")
    }

}