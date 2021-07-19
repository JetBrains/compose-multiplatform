/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
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

        assertEquals("-4", (root.children[0] as HTMLElement).style.order)
        assertEquals("3", (root.children[1] as HTMLElement).style.order)
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

        assertEquals("3", (root.children[0] as HTMLElement).style.flexGrow)
        assertEquals("2.5", (root.children[1] as HTMLElement).style.flexGrow)
        assertEquals("100", (root.children[2] as HTMLElement).style.flexGrow)
        assertEquals("0.6", (root.children[3] as HTMLElement).style.flexGrow)
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

        assertEquals("3", (root.children[0] as HTMLElement).style.flexShrink)
        assertEquals("2.5", (root.children[1] as HTMLElement).style.flexShrink)
        assertEquals("100", (root.children[2] as HTMLElement).style.flexShrink)
        assertEquals("0.6", (root.children[3] as HTMLElement).style.flexShrink)
    }

    @Test
    fun flexFlow() = runTest {
        val flexWraps = listOf(
            FlexWrap.Wrap,
            FlexWrap.Nowrap,
            FlexWrap.WrapReverse
        )

        val flexDirections = listOf(
            FlexDirection.Row,
            FlexDirection.RowReverse,
            FlexDirection.Column,
            FlexDirection.ColumnReverse
        )

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
                    "${flexDirection} ${flexWrap}",
                    (root.children[3 * i + j % 3] as HTMLElement).style.flexFlow
                )
            }
        }
    }

    @Test
    fun justifyContent() = runTest {
        val enumValues = listOf(
            JustifyContent.Center,
            JustifyContent.Start,
            JustifyContent.End,
            JustifyContent.FlexStart,
            JustifyContent.FlexEnd,
            JustifyContent.Left,
            JustifyContent.Right,
            JustifyContent.Normal,
            JustifyContent.SpaceBetween,
            JustifyContent.SpaceAround,
            JustifyContent.SpaceEvenly,
            JustifyContent.Stretch,
            JustifyContent.Inherit,
            JustifyContent.Initial,
            JustifyContent.Unset,
            JustifyContent.SafeCenter,
            JustifyContent.UnsafeCenter
        )

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
                "${justifyContent}",
                (root.children[index] as HTMLElement).style.justifyContent
            )
        }
    }

    @Test
    fun alignSelf() = runTest {
        val enumValues = listOf(
            AlignSelf.Auto,
            AlignSelf.Normal,
            AlignSelf.Center,
            AlignSelf.Start,
            AlignSelf.End,
            AlignSelf.SelfStart,
            AlignSelf.SelfEnd,
            AlignSelf.FlexStart,
            AlignSelf.FlexEnd,
            AlignSelf.Baseline,
            AlignSelf.Stretch,
            AlignSelf.SafeCenter,
            AlignSelf.UnsafeCenter,
            AlignSelf.Inherit,
            AlignSelf.Initial,
            AlignSelf.Unset
        )

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
                "${alignSelf}",
                (root.children[index] as HTMLElement).style.alignSelf
            )
        }
    }

    @Test
    fun alignItems() = runTest {
        val enumValues = listOf(
            AlignItems.Normal,
            AlignItems.Stretch,
            AlignItems.Center,
            AlignItems.Start,
            AlignItems.End,
            AlignItems.FlexStart,
            AlignItems.FlexEnd,
            AlignItems.Baseline,
            AlignItems.SafeCenter,
            AlignItems.UnsafeCenter,
            AlignItems.Inherit,
            AlignItems.Initial,
            AlignItems.Unset
        )

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
                "${alignItems}",
                (root.children[index] as HTMLElement).style.alignItems
            )
        }
    }

    @Test
    fun alignContent() = runTest {
        val enumValues = listOf(
            AlignContent.Center,
            AlignContent.Start,
            AlignContent.End,
            AlignContent.FlexStart,
            AlignContent.FlexEnd,
            AlignContent.Baseline,
            AlignContent.SafeCenter,
            AlignContent.UnsafeCenter,
            AlignContent.SpaceBetween,
            AlignContent.SpaceAround,
            AlignContent.SpaceEvenly,
            AlignContent.Stretch,
            AlignContent.Inherit,
            AlignContent.Initial,
            AlignContent.Unset
        )

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
                "${alignContent}",
                (root.children[index] as HTMLElement).style.alignContent
            )
        }
    }

    @Test
    fun flexDirection() = runTest {
        val enumValues = listOf(
            FlexDirection.Row,
            FlexDirection.RowReverse,
            FlexDirection.Column,
            FlexDirection.ColumnReverse
        )

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
                "${displayStyle}",
                (root.children[index] as HTMLElement).style.flexDirection
            )
        }
    }


    @Test
    fun flexWrap() = runTest {
        val enumValues = listOf(
            FlexWrap.Wrap,
            FlexWrap.Nowrap,
            FlexWrap.WrapReverse
        )

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
                "${displayStyle}",
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

        assertEquals("10em", (root.children[0] as HTMLElement).style.flexBasis)
        assertEquals("auto", (root.children[1] as HTMLElement).style.flexBasis)
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

        assertEquals("auto", (root.children[0] as HTMLElement).style.flexBasis)
        assertEquals("initial", (root.children[1] as HTMLElement).style.flexBasis)
        assertEquals("2", (root.children[2] as HTMLElement).style.flexGrow)
        assertEquals("10em", (root.children[3] as HTMLElement).style.flexBasis)
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

        (root.children[0] as HTMLElement).let {
            assertEquals("3", it.style.flexGrow)
            assertEquals("30px", it.style.flexBasis)
        }

        (root.children[1] as HTMLElement).let {
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

        (root.children[0] as HTMLElement).let {
            assertEquals("2", it.style.flexGrow)
            assertEquals("3", it.style.flexShrink)
            assertEquals("10%", it.style.flexBasis)
        }
    }

}