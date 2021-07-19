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
            Wrap,
            Nowrap,
            WrapReverse
        )

        val flexDirections = listOf(
            Row,
            RowReverse,
            Column,
            ColumnReverse
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
        composition {
            Span({ style { justifyContent(Center) } })
            Span({ style { justifyContent(Start) } })
            Span({ style { justifyContent(End) } })
            Span({ style { justifyContent(FlexStart) } })
            Span({ style { justifyContent(FlexEnd) } })
            Span({ style { justifyContent(Left) } })
            Span({ style { justifyContent(Right) } })
            Span({ style { justifyContent(Normal) } })
            Span({ style { justifyContent(SpaceBetween) } })
            Span({ style { justifyContent(SpaceAround) } })
            Span({ style { justifyContent(SpaceEvenly) } })
            Span({ style { justifyContent(Stretch) } })
            Span({ style { justifyContent(Inherit) } })
            Span({ style { justifyContent(Initial) } })
            Span({ style { justifyContent(Unset) } })
            Span({ style { justifyContent(SafeCenter) } })
            Span({ style { justifyContent(UnsafeCenter) } })
        }

        var counter = 0
        assertEquals("center", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("start", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("end", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("flex-start", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("flex-end", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("left", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("right", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("normal", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("space-between", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("space-around", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("space-evenly", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("stretch", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("inherit", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("initial", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("unset", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("safe center", (root.children[counter++] as HTMLElement).style.justifyContent)
        assertEquals("unsafe center", (root.children[counter] as HTMLElement).style.justifyContent)
    }

    @Test
    fun alignSelf() = runTest {
        composition {
            Span({ style { alignSelf(Auto) } })
            Span({ style { alignSelf(Normal) } })
            Span({ style { alignSelf(Center) } })
            Span({ style { alignSelf(Start) } })
            Span({ style { alignSelf(End) } })
            Span({ style { alignSelf(SelfStart) } })
            Span({ style { alignSelf(SelfEnd) } })
            Span({ style { alignSelf(FlexStart) } })
            Span({ style { alignSelf(FlexEnd) } })
            Span({ style { alignSelf(Baseline) } })
            Span({ style { alignSelf(Stretch) } })
            Span({ style { alignSelf(SafeCenter) } })
            Span({ style { alignSelf(UnsafeCenter) } })
            Span({ style { alignSelf(Inherit) } })
            Span({ style { alignSelf(Initial) } })
            Span({ style { alignSelf(Unset) } })
        }

        var counter = 0
        assertEquals("auto", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("normal", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("center", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("start", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("end", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("self-start", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("self-end", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("flex-start", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("flex-end", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("baseline", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("stretch", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("safe center", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("unsafe center", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("inherit", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("initial", (root.children[counter++] as HTMLElement).style.alignSelf)
        assertEquals("unset", (root.children[counter] as HTMLElement).style.alignSelf)
    }

    @Test
    fun alignItems() = runTest {
        composition {
            Span({ style { alignItems(Normal) } })
            Span({ style { alignItems(Stretch) } })
            Span({ style { alignItems(Center) } })
            Span({ style { alignItems(Start) } })
            Span({ style { alignItems(End) } })
            Span({ style { alignItems(FlexStart) } })
            Span({ style { alignItems(FlexEnd) } })
            Span({ style { alignItems(Baseline) } })
            Span({ style { alignItems(SafeCenter) } })
            Span({ style { alignItems(UnsafeCenter) } })
            Span({ style { alignItems(Inherit) } })
            Span({ style { alignItems(Initial) } })
            Span({ style { alignItems(Unset) } })
        }

        var counter = 0
        assertEquals("normal", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("stretch", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("center", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("start", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("end", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("flex-start", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("flex-end", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("baseline", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("safe center", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("unsafe center", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("inherit", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("initial", (root.children[counter++] as HTMLElement).style.alignItems)
        assertEquals("unset", (root.children[counter] as HTMLElement).style.alignItems)
    }

    @Test
    fun alignContent() = runTest {
        val enumValues = listOf(
            Center,
            Start,
            End,
            FlexStart,
            FlexEnd,
            Baseline,
            SafeCenter,
            UnsafeCenter,
            SpaceBetween,
            SpaceAround,
            SpaceEvenly,
            Stretch,
            Inherit,
            Initial,
            Unset
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
        composition {
            Span({ style { flexDirection(Row) } })
            Span({ style { flexDirection(RowReverse) } })
            Span({ style { flexDirection(Column) } })
            Span({ style { flexDirection(ColumnReverse) } })
        }

        var counter = 0
        assertEquals("row", (root.children[counter++] as HTMLElement).style.flexDirection)
        assertEquals("row-reverse", (root.children[counter++] as HTMLElement).style.flexDirection)
        assertEquals("column", (root.children[counter++] as HTMLElement).style.flexDirection)
        assertEquals("column-reverse", (root.children[counter] as HTMLElement).style.flexDirection)
    }


    @Test
    fun flexWrap() = runTest {
        val enumValues = listOf(
            Wrap,
            Nowrap,
            WrapReverse
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