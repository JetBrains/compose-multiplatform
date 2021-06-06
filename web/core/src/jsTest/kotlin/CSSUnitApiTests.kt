/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.css.CSSUnitValue
import org.jetbrains.compose.web.css.CSSchValue
import org.jetbrains.compose.web.css.CSScmValue
import org.jetbrains.compose.web.css.CSSdegValue
import org.jetbrains.compose.web.css.CSSdpcmValue
import org.jetbrains.compose.web.css.CSSdpiValue
import org.jetbrains.compose.web.css.CSSdppxValue
import org.jetbrains.compose.web.css.CSSemValue
import org.jetbrains.compose.web.css.CSSfrValue
import org.jetbrains.compose.web.css.CSSgradValue
import org.jetbrains.compose.web.css.CSSmmValue
import org.jetbrains.compose.web.css.CSSmsValue
import org.jetbrains.compose.web.css.CSSnumberValue
import org.jetbrains.compose.web.css.CSSpcValue
import org.jetbrains.compose.web.css.CSSpercentValue
import org.jetbrains.compose.web.css.CSSptValue
import org.jetbrains.compose.web.css.CSSpxValue
import org.jetbrains.compose.web.css.CSSradValue
import org.jetbrains.compose.web.css.CSSremValue
import org.jetbrains.compose.web.css.CSSsValue
import org.jetbrains.compose.web.css.CSSturnValue
import org.jetbrains.compose.web.css.CSSvhValue
import org.jetbrains.compose.web.css.CSSvmaxValue
import org.jetbrains.compose.web.css.CSSvminValue
import org.jetbrains.compose.web.css.CSSvwValue
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
    fun postfixInvocation() {
        4.number.assertStructure(CSSnumberValue(4), "number postfix")
        4.percent.assertStructure(CSSpercentValue(4), "percent posfix")

        4.em.assertStructure(CSSemValue(4), "em postfix")
        4.ch.assertStructure(CSSchValue(4), "ch postfix")

        4.cssRem.assertStructure(CSSremValue(4))

        4.vw.assertStructure(CSSvwValue(4),"vw postfix")
        4.vh.assertStructure(CSSvhValue(4), "vh postfix")

        4.vmin.assertStructure(CSSvminValue(4), "vmin postfix")
        4.vmax.assertStructure(CSSvmaxValue(4), "vmax postfix")
        4.cm.assertStructure(CSScmValue(4), "cm postfix")
        4.mm.assertStructure(CSSmmValue(4), "mm postfix")

        4.pt.assertStructure(CSSptValue(4), "pt postfix")
        4.pc.assertStructure(CSSpcValue(4), "pc postfix")
        4.px.assertStructure(CSSpxValue(4), "px postfix")

        4.deg.assertStructure(CSSdegValue(4), "deg postfix")
        4.grad.assertStructure(CSSgradValue(4), "grad postfix")
        4.rad.assertStructure(CSSradValue(4), "rad postfix")
        4.turn.assertStructure(CSSturnValue(4), "turn postfix")

        4.s.assertStructure(CSSsValue(4), "s postfix")
        4.ms.assertStructure(CSSmsValue(4), "ms postfix")

        4.dpi.assertStructure(CSSdpiValue(4), "dpi postfix")
        4.dpcm.assertStructure(CSSdpcmValue(4), "dpcm postfix")
        4.dppx.assertStructure(CSSdppxValue(4), "dppx postfix")

        4.fr.assertStructure(CSSfrValue(4), "fr postfix")
    }

}