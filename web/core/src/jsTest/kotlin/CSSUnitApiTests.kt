/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

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

    @Test
    fun postfixInvocation() {
        // TODO: review what exactly number does - most likely we don't need it in our ecosystem
        assertEquals("4number", 4.number.asString())

        assertEquals("4%", 4.percent.asString())

        assertEquals("4em", 4.em.asString())
        assertEquals("4ch", 4.ch.asString())

        assertEquals("4rem", 4.cssRem.asString())

        assertEquals("4vw", 4.vw.asString())
        assertEquals("4vh", 4.vh.asString())

        assertEquals("4vmin", 4.vmin.asString())
        assertEquals("4vmax", 4.vmax.asString())
        assertEquals("4cm", 4.cm.asString())
        assertEquals("4mm", 4.mm.asString())

        assertEquals("4pt", 4.pt.asString())
        assertEquals("4pc", 4.pc.asString())
        assertEquals("4px", 4.px.asString())

        assertEquals("4deg", 4.deg.asString())
        assertEquals("4grad", 4.grad.asString())
        assertEquals("4rad", 4.rad.asString())
        assertEquals("4turn", 4.turn.asString())

        assertEquals("4s", 4.s.asString())
        assertEquals("4ms", 4.ms.asString())

        assertEquals("4dpi", 4.dpi.asString())
        assertEquals("4dpcm", 4.dpcm.asString())
        assertEquals("4dppx", 4.dppx.asString())

        assertEquals("4fr", 4.fr.asString())
    }

}