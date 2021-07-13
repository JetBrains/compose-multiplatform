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
    fun stylesOrder() = runTest {
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

        assertEquals("-4", (root.children[0] as HTMLElement).style.order)
        assertEquals("3", (root.children[1] as HTMLElement).style.order)
    }

    @Test
    fun stylesFlexGrow() = runTest {
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

        assertEquals("3", (root.children[0] as HTMLElement).style.flexGrow)
        assertEquals("2.5", (root.children[1] as HTMLElement).style.flexGrow)
        assertEquals("100", (root.children[2] as HTMLElement).style.flexGrow)
        assertEquals("0.6", (root.children[3] as HTMLElement).style.flexGrow)
    }

    @Test
    fun stylesFlexShrink() = runTest {
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

        assertEquals("3", (root.children[0] as HTMLElement).style.flexShrink)
        assertEquals("2.5", (root.children[1] as HTMLElement).style.flexShrink)
        assertEquals("100", (root.children[2] as HTMLElement).style.flexShrink)
        assertEquals("0.6", (root.children[3] as HTMLElement).style.flexShrink)
    }

    @Test
    fun stylesFlexFlow() = runTest {
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
    fun stylesJustifyContent() = runTest {
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
    fun stylesAlignSelf() = runTest {
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
    fun stylesAlignItems() = runTest {
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
    fun stylesAlignContent() = runTest {
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
    fun stylesFlexDirection() = runTest {
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
    fun stylesFlexWrap() = runTest {
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
    fun stylesFlexBasis() = runTest {
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

        assertEquals("10em", (root.children[0] as HTMLElement).style.flexBasis)
        assertEquals("auto", (root.children[1] as HTMLElement).style.flexBasis)
    }
}