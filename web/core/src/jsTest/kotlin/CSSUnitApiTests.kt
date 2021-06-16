/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.ch
import org.jetbrains.compose.web.css.cm
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.deg
import org.jetbrains.compose.web.css.div
import org.jetbrains.compose.web.css.dpcm
import org.jetbrains.compose.web.css.dpi
import org.jetbrains.compose.web.css.dppx
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.fr
import org.jetbrains.compose.web.css.grad
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.minus
import org.jetbrains.compose.web.css.mm
import org.jetbrains.compose.web.css.ms
import org.jetbrains.compose.web.css.number
import org.jetbrains.compose.web.css.pc
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.plus
import org.jetbrains.compose.web.css.pt
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rad
import org.jetbrains.compose.web.css.s
import org.jetbrains.compose.web.css.times
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.turn
import org.jetbrains.compose.web.css.vh
import org.jetbrains.compose.web.css.vmax
import org.jetbrains.compose.web.css.vmin
import org.jetbrains.compose.web.css.vw
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSUnitApiTests {
    // TODO: Cover CSS.Q, CSS.khz and CSS.hz after we'll get rid from polyfill

    @Test
    fun postfixInvocation() {
        // TODO: review what exactly number does - most likely we don't need it in our ecosystem
        assertEquals("4number", 4.number.toString())

        assertEquals("4%", 4.percent.toString())

        assertEquals("4em", 4.em.toString())
        assertEquals("4ch", 4.ch.toString())

        assertEquals("4rem", 4.cssRem.toString())

        assertEquals("4vw", 4.vw.toString())
        assertEquals("4vh", 4.vh.toString())

        assertEquals("4vmin", 4.vmin.toString())
        assertEquals("4vmax", 4.vmax.toString())
        assertEquals("4cm", 4.cm.toString())
        assertEquals("4mm", 4.mm.toString())

        assertEquals("4pt", 4.pt.toString())
        assertEquals("4pc", 4.pc.toString())
        assertEquals("4px", 4.px.toString())

        assertEquals("4deg", 4.deg.toString())
        assertEquals("4grad", 4.grad.toString())
        assertEquals("4rad", 4.rad.toString())
        assertEquals("4turn", 4.turn.toString())

        assertEquals("4s", 4.s.toString())
        assertEquals("4ms", 4.ms.toString())

        assertEquals("4dpi", 4.dpi.toString())
        assertEquals("4dpcm", 4.dpcm.toString())
        assertEquals("4dppx", 4.dppx.toString())

        assertEquals("4fr", 4.fr.toString())
    }

    @Test
    fun id() {
        // TODO: review what exactly number does - most likely we don't need it in our ecosystem
        assertEquals(4.number, 4.number)

        assertEquals(4.percent, 4.percent)

        assertEquals(4.em, 4.em)
        assertEquals(4.ch, 4.ch)

        assertEquals(4.cssRem, 4.cssRem)

        assertEquals(4.vw, 4.vw)
        assertEquals(4.vh, 4.vh)

        assertEquals(4.vmin, 4.vmin)
        assertEquals(4.vmax, 4.vmax)
        assertEquals(4.cm, 4.cm)
        assertEquals(4.mm, 4.mm)

        assertEquals(4.pt, 4.pt)
        assertEquals(4.pc, 4.pc)
        assertEquals(4.px, 4.px)

        assertEquals(4.deg, 4.deg)
        assertEquals(4.grad, 4.grad)
        assertEquals(4.rad, 4.rad)
        assertEquals(4.turn, 4.turn)

        assertEquals(4.s, 4.s)
        assertEquals(4.ms, 4.ms)

        assertEquals(4.dpi, 4.dpi)
        assertEquals(4.dpcm, 4.dpcm)
        assertEquals(4.dppx, 4.dppx)

        assertEquals(4.fr, 4.fr)
    }

    @Test
    fun arithmeticMultiplicationLeft() {
        assertEquals(16.percent, 4.percent * 4)

        assertEquals(16.em, 4.em * 4)
        assertEquals(16.ch, 4.ch * 4)

        assertEquals(16.cssRem, 4.cssRem * 4)

        assertEquals(16.vw, 4.vw * 4)
        assertEquals(16.vh, 4.vh * 4)

        assertEquals(16.vmin, 4.vmin * 4)
        assertEquals(16.vmax, 4.vmax * 4)
        assertEquals(16.cm, 4.cm * 4)
        assertEquals(16.mm, 4.mm * 4)

        assertEquals(16.pt, 4.pt * 4)
        assertEquals(16.pc, 4.pc * 4)
        assertEquals(16.px, 4.px * 4)

        assertEquals(16.deg, 4.deg * 4)
        assertEquals(16.grad, 4.grad * 4)
        assertEquals(16.rad, 4.rad * 4)
        assertEquals(16.turn, 4.turn * 4)

        assertEquals(16.s, 4.s * 4)
        assertEquals(16.ms, 4.ms * 4)

        assertEquals(16.dpi, 4.dpi * 4)
        assertEquals(16.dpcm, 4.dpcm * 4)
        assertEquals(16.dppx, 4.dppx * 4)

        assertEquals(16.fr, 4.fr * 4)
    }

    @Test
    fun arithmeticDivisionLeft() {
        assertEquals(1.percent, 4.percent / 4)

        assertEquals(1.em, 4.em / 4)
        assertEquals(1.ch, 4.ch / 4)

        assertEquals(1.cssRem, 4.cssRem / 4)

        assertEquals(1.vw, 4.vw / 4)
        assertEquals(1.vh, 4.vh / 4)

        assertEquals(1.vmin, 4.vmin / 4)
        assertEquals(1.vmax, 4.vmax / 4)
        assertEquals(1.cm, 4.cm / 4)
        assertEquals(1.mm, 4.mm / 4)

        assertEquals(1.pt, 4.pt / 4)
        assertEquals(1.pc, 4.pc / 4)
        assertEquals(1.px, 4.px / 4)

        assertEquals(1.deg, 4.deg / 4)
        assertEquals(1.grad, 4.grad / 4)
        assertEquals(1.rad, 4.rad / 4)
        assertEquals(1.turn, 4.turn / 4)

        assertEquals(1.s, 4.s / 4)
        assertEquals(1.ms, 4.ms / 4)

        assertEquals(1.dpi, 4.dpi / 4)
        assertEquals(1.dpcm, 4.dpcm / 4)
        assertEquals(1.dppx, 4.dppx / 4)

        assertEquals(1.fr, 4.fr / 4)
    }

    @Test
    fun arithmeticMultiplicationRight() {
        assertEquals(12.percent, 3 * 4.percent)

        assertEquals(12.em, 3 * 4.em)
        assertEquals(12.ch, 3 * 4.ch)

        assertEquals(12.cssRem, 3 * 4.cssRem)

        assertEquals(12.vw, 3 * 4.vw)
        assertEquals(12.vh, 3 * 4.vh)

        assertEquals(12.vmin, 3 * 4.vmin)
        assertEquals(12.vmax, 3 * 4.vmax)
        assertEquals(12.cm, 3 * 4.cm)
        assertEquals(12.mm, 3 * 4.mm)

        assertEquals(12.pt, 3 * 4.pt)
        assertEquals(12.pc, 3 * 4.pc)
        assertEquals(12.px, 3 * 4.px)

        assertEquals(12.deg, 3 * 4.deg)
        assertEquals(12.grad, 3 * 4.grad)
        assertEquals(12.rad, 3 * 4.rad)
        assertEquals(12.turn, 3 * 4.turn)

        assertEquals(12.s, 3 * 4.s)
        assertEquals(12.ms, 3 * 4.ms)

        assertEquals(12.dpi, 3 * 4.dpi)
        assertEquals(12.dpcm, 3 * 4.dpcm)
        assertEquals(12.dppx, 3 * 4.dppx)

        assertEquals(12.fr, 3 * 4.fr)
    }

    @Test
    fun addHomogenous() {
        assertEquals(13.percent, 7.percent + 4.percent + 2.percent)

        assertEquals(13.em, 7.em + 4.em + 2.em)
        assertEquals(13.ch, 7.ch + 4.ch + 2.ch)

        assertEquals(13.cssRem, 7.cssRem + 4.cssRem + 2.cssRem)

        assertEquals(13.vw, 7.vw + 4.vw + 2.vw)
        assertEquals(13.vh, 7.vh + 4.vh + 2.vh)

        assertEquals(13.vmin, 7.vmin + 4.vmin + 2.vmin)
        assertEquals(13.vmax, 7.vmax + 4.vmax + 2.vmax)
        assertEquals(13.cm, 7.cm + 4.cm + 2.cm)
        assertEquals(13.mm, 7.mm + 4.mm + 2.mm)

        assertEquals(13.pt, 7.pt + 4.pt + 2.pt)
        assertEquals(13.pc, 7.pc + 4.pc + 2.pc)
        assertEquals(13.px, 7.px + 4.px + 2.px)

        assertEquals(13.deg, 7.deg + 4.deg + 2.deg)
        assertEquals(13.grad, 7.grad + 4.grad + 2.grad)
        assertEquals(13.rad, 7.rad + 4.rad + 2.rad)
        assertEquals(13.turn, 7.turn + 4.turn + 2.turn)

        assertEquals(13.s, 7.s + 4.s + 2.s)
        assertEquals(13.ms, 7.ms + 4.ms + 2.ms)

        assertEquals(13.dpi, 7.dpi + 4.dpi + 2.dpi)
        assertEquals(13.dpcm, 7.dpcm + 4.dpcm + 2.dpcm)
        assertEquals(13.dppx, 7.dppx + 4.dppx + 2.dppx)

        assertEquals(13.fr, 7.fr + 4.fr + 2.fr)
    }

    @Test
    fun substractHomogenous() {
        assertEquals(1.percent, 7.percent - 4.percent - 2.percent)

        assertEquals(1.em, 7.em - 4.em - 2.em)
        assertEquals(1.ch, 7.ch - 4.ch - 2.ch)

        assertEquals(1.cssRem, 7.cssRem - 4.cssRem - 2.cssRem)

        assertEquals(1.vw, 7.vw - 4.vw - 2.vw)
        assertEquals(1.vh, 7.vh - 4.vh - 2.vh)

        assertEquals(1.vmin, 7.vmin - 4.vmin - 2.vmin)
        assertEquals(1.vmax, 7.vmax - 4.vmax - 2.vmax)
        assertEquals(1.cm, 7.cm - 4.cm - 2.cm)
        assertEquals(1.mm, 7.mm - 4.mm - 2.mm)

        assertEquals(1.pt, 7.pt - 4.pt - 2.pt)
        assertEquals(1.pc, 7.pc - 4.pc - 2.pc)
        assertEquals(1.px, 7.px - 4.px - 2.px)

        assertEquals(1.deg, 7.deg - 4.deg - 2.deg)
        assertEquals(1.grad, 7.grad - 4.grad - 2.grad)
        assertEquals(1.rad, 7.rad - 4.rad - 2.rad)
        assertEquals(1.turn, 7.turn - 4.turn - 2.turn)

        assertEquals(1.s, 7.s - 4.s - 2.s)
        assertEquals(1.ms, 7.ms - 4.ms - 2.ms)

        assertEquals(1.dpi, 7.dpi - 4.dpi - 2.dpi)
        assertEquals(1.dpcm, 7.dpcm - 4.dpcm - 2.dpcm)
        assertEquals(1.dppx, 7.dppx - 4.dppx - 2.dppx)

        assertEquals(1.fr, 7.fr - 4.fr - 2.fr)
    }

    @Test
    fun staticEvaluation() = runTest {
        composition {
            Div({
                var a = 5.px
                style {
                    val b = a + 3.px
                    left(a)
                    top(b)
                    a = 20.px
                }
            })
        }


        assertEquals("5px", (root.children[0] as HTMLElement).style.left)
        assertEquals("8px", (root.children[0] as HTMLElement).style.top)
    }
}