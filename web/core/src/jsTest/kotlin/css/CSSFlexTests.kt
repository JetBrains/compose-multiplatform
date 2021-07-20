/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.core.tests.values
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSFlexTests {

    @Test
    fun order() = runTest {
        composition {
            Div(
                {
                    style {
                        order(-4)
                    }
                }
            )
            Div(
                {
                    style {
                        order(3)
                    }
                }
            )
        }

        assertEquals("-4", (nextChild()).style.order)
        assertEquals("3", (nextChild()).style.order)
    }

    @Test
    fun flexGrow() = runTest {
        composition {
            Div(
                {
                    style {
                        flexGrow(3)
                    }
                }
            )
            Div(
                {
                    style {
                        flexGrow(2.5)
                    }
                }
            )
            Div(
                {
                    style {
                        flexGrow(1e2)
                    }
                }
            )
            Div(
                {
                    style {
                        flexGrow(.6)
                    }
                }
            )
        }

        assertEquals("3", (nextChild()).style.flexGrow)
        assertEquals("2.5", (nextChild()).style.flexGrow)
        assertEquals("100", (nextChild()).style.flexGrow)
        assertEquals("0.6", (nextChild()).style.flexGrow)
    }

    @Test
    fun flexShrink() = runTest {
        composition {
            Div(
                {
                    style {
                        flexShrink(3)
                    }
                }
            )
            Div(
                {
                    style {
                        flexShrink(2.5)
                    }
                }
            )
            Div(
                {
                    style {
                        flexShrink(1e2)
                    }
                }
            )
            Div(
                {
                    style {
                        flexShrink(.6)
                    }
                }
            )
        }

        assertEquals("3", (nextChild()).style.flexShrink)
        assertEquals("2.5", (nextChild()).style.flexShrink)
        assertEquals("100", (nextChild()).style.flexShrink)
        assertEquals("0.6", (nextChild()).style.flexShrink)
    }

    @Test
    fun flexFlow() = runTest {
        val flexWraps = FlexWrap.values()
        val flexDirections = FlexDirection.values()
        composition {
            flexDirections.forEach { flexDirection ->
                flexWraps.forEach { flexWrap ->
                    Span(
                        {
                            style {
                                flexFlow(flexDirection, flexWrap)
                            }
                        }
                    )
                }
            }
        }

        flexDirections.forEachIndexed { i, flexDirection ->
            flexWraps.forEachIndexed { j, flexWrap ->
                assertEquals(
                    "${flexDirection.value} ${flexWrap.value}",
                    (root.children[3 * i + j % 3] as HTMLElement).style.flexFlow
                )
            }
        }
    }

    @Test
    fun justifyContent() = runTest {
        val enumValues = JustifyContent.values()
        composition {
            enumValues.forEach { justifyContent ->
                Span(
                    {
                        style {
                            justifyContent(justifyContent)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, justifyContent ->
            assertEquals(
                "${justifyContent.value}",
                (root.children[index] as HTMLElement).style.justifyContent
            )
        }
    }

    @Test
    fun alignSelf() = runTest {
        val enumValues = AlignSelf.values()
        composition {
            enumValues.forEach { alignSelf ->
                Span(
                    {
                        style {
                            alignSelf(alignSelf)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, alignSelf ->
            assertEquals(
                "${alignSelf.value}",
                (root.children[index] as HTMLElement).style.alignSelf
            )
        }
    }

    @Test
    fun alignItems() = runTest {
        val enumValues = AlignItems.values()
        composition {
            enumValues.forEach { alignItems ->
                Span(
                    {
                        style {
                            alignItems(alignItems)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, alignItems ->
            assertEquals(
                "${alignItems.value}",
                (root.children[index] as HTMLElement).style.alignItems
            )
        }
    }

    @Test
    fun alignContent() = runTest {
        val enumValues = AlignContent.values()
        composition {
            enumValues.forEach { alignContent ->
                Span(
                    {
                        style {
                            alignContent(alignContent)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, alignContent ->
            assertEquals(
                "${alignContent.value}",
                (root.children[index] as HTMLElement).style.alignContent
            )
        }
    }

    @Test
    fun flexDirection() = runTest {
        val enumValues = FlexDirection.values()
        composition {
            enumValues.forEach { flexDirection ->
                Span(
                    {
                        style {
                            flexDirection(flexDirection)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, displayStyle ->
            assertEquals(
                "${displayStyle.value}",
                (root.children[index] as HTMLElement).style.flexDirection
            )
        }
    }


    @Test
    fun flexWrap() = runTest {
        val enumValues = FlexWrap.values()
        composition {
            enumValues.forEach { flexWrap ->
                Span(
                    {
                        style {
                            flexWrap(flexWrap)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, displayStyle ->
            assertEquals(
                "${displayStyle.value}",
                (root.children[index] as HTMLElement).style.flexWrap
            )
        }
    }

    @Test
    fun flexBasis() = runTest {
        composition {
            Span({
                style {
                    flexBasis(10.em)
                }
            })
            Span({
                style {
                    flexBasis("auto")
                }
            })
        }

        assertEquals("10em", (nextChild()).style.flexBasis)
        assertEquals("auto", (nextChild()).style.flexBasis)
    }

    @Test
    fun flexOneValue() = runTest {
        composition {
            Span({
                style {
                    flex("auto")
                }
            })
            Span({
                style {
                    flex("initial")
                }
            })
            Span({
                style {
                    flex(2)
                }
            })
            Span({
                style {
                    flex(10.em)
                }
            })
        }

        assertEquals("auto", (nextChild()).style.flexBasis)
        assertEquals("initial", (nextChild()).style.flexBasis)
        assertEquals("2", (nextChild()).style.flexGrow)
        assertEquals("10em", (nextChild()).style.flexBasis)
    }

    @Test
    fun flexTwoValues() = runTest {
        composition {
            Span({
                style {
                    flex(3, 30.px)
                }
            })
            Span({
                style {
                    flex(3, 5)
                }
            })
        }

        (nextChild()).let {
            assertEquals("3", it.style.flexGrow)
            assertEquals("30px", it.style.flexBasis)
        }

        (nextChild()).let {
            assertEquals("3", it.style.flexGrow)
            assertEquals("5", it.style.flexShrink)
        }
    }

    @Test
    fun flexThreeValues() = runTest {
        composition {
            Span({
                style {
                    flex(2, 3, 10.percent)
                }
            })
        }

        (nextChild()).let {
            assertEquals("2", it.style.flexGrow)
            assertEquals("3", it.style.flexShrink)
            assertEquals("10%", it.style.flexBasis)
        }
    }

}