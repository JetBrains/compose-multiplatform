/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.testutils.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import kotlin.test.Test
import kotlin.test.assertEquals

class GridColumnTests {
    @Test
    fun gridColumnOneValue() = runTest {
        composition {
            Div({ style { gridColumn("1") } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("1", el.gridColumnStart)
        assertEquals("auto", el.gridColumnEnd)
    }

    @Test
    fun gridColumnTwoValues() = runTest {
        composition {
            Div({ style { gridColumn(1, 3) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("1", el.gridColumnStart)
        assertEquals("3", el.gridColumnEnd)
    }

    @Test
    fun gridColumnLineNames() = runTest {
        composition {
            Div({ style { gridColumn("main-start") } })
            Div({ style { gridColumn("main-start", "main-end") } })
        }

        assertEquals("main-start", nextChild().style.asDynamic().gridColumnStart)

        nextChild().style.apply {
            assertEquals("main-start", asDynamic().gridColumnStart)
            assertEquals("main-end", asDynamic().gridColumnEnd)
        }
    }


    @Test
    fun gridColumnGlobalValues() = runTest {
        composition {
            Div({ style { gridColumn("inherit") } })
            Div({ style { gridColumn("initial") } })
            Div({ style { gridColumn("revert") } })
            Div({ style { gridColumn("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("initial", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("revert", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("unset", nextChild().style.asDynamic().gridColumnStart)
    }
}

class GridColumnEndTests {
    @Test
    fun gridColumnEndOneValue() = runTest {
        composition {
            Div({ style { gridColumnEnd("1") } })
            Div({ style { gridColumnEnd("somegridarea") } })
        }

        assertEquals("1", nextChild().style.asDynamic().gridColumnEnd)
        assertEquals("somegridarea", nextChild().style.asDynamic().gridColumnEnd)
    }

    @Test
    fun gridColumnEndIntValue() = runTest {
        composition {
            Div({ style { gridColumnEnd(-4) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("-4", el.gridColumnEnd)
    }

    @Test
    fun gridColumnEndGlobalValues() = runTest {
        composition {
            Div({ style { gridColumn("inherit") } })
            Div({ style { gridColumn("initial") } })
            Div({ style { gridColumn("revert") } })
            Div({ style { gridColumn("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridColumnEnd)
        assertEquals("initial", nextChild().style.asDynamic().gridColumnEnd)
        assertEquals("revert", nextChild().style.asDynamic().gridColumnEnd)
        assertEquals("unset", nextChild().style.asDynamic().gridColumnEnd)
    }
}

class GridColumnStartTests {
    @Test
    fun gridColumnStartOneValue() = runTest {
        composition {
            Div({ style { gridColumnStart("1") } })
            Div({ style { gridColumnStart("somegridarea") } })
        }

        assertEquals("1", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("somegridarea", nextChild().style.asDynamic().gridColumnStart)
    }

    @Test
    fun gridColumnStartIntValue() = runTest {
        composition {
            Div({ style { gridColumnStart(-4) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("-4", el.gridColumnStart)
    }

    @Test
    fun gridColumnStartGlobalValues() = runTest {
        composition {
            Div({ style { gridColumn("inherit") } })
            Div({ style { gridColumn("initial") } })
            Div({ style { gridColumn("revert") } })
            Div({ style { gridColumn("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("initial", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("revert", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("unset", nextChild().style.asDynamic().gridColumnStart)
    }
}

class GridRowStartTests {
    @Test
    fun gridRowStartOneValue() = runTest {
        composition {
            Div({ style { gridRowStart("1") } })
            Div({ style { gridRowStart("somegridarea") } })
        }

        assertEquals("1", nextChild().style.asDynamic().gridRowStart)
        assertEquals("somegridarea", nextChild().style.asDynamic().gridRowStart)
    }

    @Test
    fun gridRowStartIntValue() = runTest {
        composition {
            Div({ style { gridRowStart(-4) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("-4", el.gridRowStart)
    }

    @Test
    fun gridRowStartGlobalValues() = runTest {
        composition {
            Div({ style { gridRow("inherit") } })
            Div({ style { gridRow("initial") } })
            Div({ style { gridRow("revert") } })
            Div({ style { gridRow("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridRowStart)
        assertEquals("initial", nextChild().style.asDynamic().gridRowStart)
        assertEquals("revert", nextChild().style.asDynamic().gridRowStart)
        assertEquals("unset", nextChild().style.asDynamic().gridRowStart)
    }
}

class GridRowEndTests {
    @Test
    fun gridRowEndOneValue() = runTest {
        composition {
            Div({ style { gridRowEnd("1") } })
            Div({ style { gridRowEnd("somegridarea") } })
        }

        assertEquals("1", nextChild().style.asDynamic().gridRowEnd)
        assertEquals("somegridarea", nextChild().style.asDynamic().gridRowEnd)
    }

    @Test
    fun gridRowEndIntValue() = runTest {
        composition {
            Div({ style { gridRowEnd(-4) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("-4", el.gridRowEnd)
    }

    @Test
    fun gridRowEndGlobalValues() = runTest {
        composition {
            Div({ style { gridRow("inherit") } })
            Div({ style { gridRow("initial") } })
            Div({ style { gridRow("revert") } })
            Div({ style { gridRow("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridRowEnd)
        assertEquals("initial", nextChild().style.asDynamic().gridRowEnd)
        assertEquals("revert", nextChild().style.asDynamic().gridRowEnd)
        assertEquals("unset", nextChild().style.asDynamic().gridRowEnd)
    }
}


class GridRawTests {

    @Test
    fun gridRowOneValue() = runTest {
        composition {
            Div({ style { gridRow("1") } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("1", el.gridRowStart)
        assertEquals("auto", el.gridRowEnd)
    }

    @Test
    fun gridRowTwoValues() = runTest {
        composition {
            Div({ style { gridRow(2, -1) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("2", el.gridRowStart)
        assertEquals("-1", el.gridRowEnd)
    }

    @Test
    fun gridRowCustomIndentValuesStrInt() = runTest {
        composition {
            Div({ style { gridRow("4 somegridarea", 6) } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("4 somegridarea", el.gridRowStart)
        assertEquals("6", el.gridRowEnd)
    }

    @Test
    fun gridRowCustomIndentValuesIntStr() = runTest {
        composition {
            Div({ style { gridRow(5, "4 somegridarea") } })
        }

        val el = nextChild().style.asDynamic()
        assertEquals("5", el.gridRowStart)
        assertEquals("4 somegridarea", el.gridRowEnd)
    }


    @Test
    fun gridRowGlobalValues() = runTest {
        composition {
            Div({ style { gridRow("inherit") } })
            Div({ style { gridRow("initial") } })
            Div({ style { gridRow("revert") } })
            Div({ style { gridRow("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridRowStart)
        assertEquals("initial", nextChild().style.asDynamic().gridRowStart)
        assertEquals("revert", nextChild().style.asDynamic().gridRowStart)
        assertEquals("unset", nextChild().style.asDynamic().gridRowStart)
    }
}

class GridTemplateRowsTests {
    @Test
    fun gridTemplateRowsGlobalValues() = runTest {
        composition {
            Div({ style { gridTemplateRows("inherit") } })
            Div({ style { gridTemplateRows("initial") } })
            Div({ style { gridTemplateRows("revert") } })
            Div({ style { gridTemplateRows("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridTemplateRows)
        assertEquals("initial", nextChild().style.asDynamic().gridTemplateRows)
        assertEquals("revert", nextChild().style.asDynamic().gridTemplateRows)
        assertEquals("unset", nextChild().style.asDynamic().gridTemplateRows)
    }

    @Test
    fun gridAutoRowsGlobalValues() = runTest {
        composition {
            Div({ style { gridAutoRows("inherit") } })
            Div({ style { gridAutoRows("initial") } })
            Div({ style { gridAutoRows("revert") } })
            Div({ style { gridAutoRows("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridAutoRows)
        assertEquals("initial", nextChild().style.asDynamic().gridAutoRows)
        assertEquals("revert", nextChild().style.asDynamic().gridAutoRows)
        assertEquals("unset", nextChild().style.asDynamic().gridAutoRows)
    }

}

class GridTemplateColumnsTests {
    @Test
    fun gridTemplateColumnsGlobalValues() = runTest {
        composition {
            Div({ style { gridTemplateColumns("inherit") } })
            Div({ style { gridTemplateColumns("initial") } })
            Div({ style { gridTemplateColumns("revert") } })
            Div({ style { gridTemplateColumns("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridTemplateColumns)
        assertEquals("initial", nextChild().style.asDynamic().gridTemplateColumns)
        assertEquals("revert", nextChild().style.asDynamic().gridTemplateColumns)
        assertEquals("unset", nextChild().style.asDynamic().gridTemplateColumns)
    }

    @Test
    fun gridAutoColumnsGlobalValues() = runTest {
        composition {
            Div({ style { gridAutoColumns("inherit") } })
            Div({ style { gridAutoColumns("initial") } })
            Div({ style { gridAutoColumns("revert") } })
            Div({ style { gridAutoColumns("unset") } })
        }

        assertEquals("inherit", nextChild().style.asDynamic().gridAutoColumns)
        assertEquals("initial", nextChild().style.asDynamic().gridAutoColumns)
        assertEquals("revert", nextChild().style.asDynamic().gridAutoColumns)
        assertEquals("unset", nextChild().style.asDynamic().gridAutoColumns)
    }
}

class GridAreaTests {
    @Test
    fun gridAreaOneValue() = runTest {
        composition {
            Div({ style { gridArea("span 3") } })
        }

        assertEquals("span 3", nextChild().style.asDynamic().gridRowStart)
    }

    @Test
    fun gridAreaTwoValues() = runTest {
        composition {
            Div({ style { gridArea("some-grid-area", "another-grid-area") } })
        }

        val el = nextChild()
        assertEquals("some-grid-area", el.style.asDynamic().gridRowStart)
        assertEquals("another-grid-area", el.style.asDynamic().gridColumnStart)
    }

    @Test
    fun gridAreaThreeValues() = runTest {
        composition {
            Div({ style { gridArea("some-grid-area", "another-grid-area", "yet-another-grid-area") } })
        }

        val el = nextChild()
        assertEquals("some-grid-area", el.style.asDynamic().gridRowStart)
        assertEquals("another-grid-area", el.style.asDynamic().gridColumnStart)
        assertEquals("yet-another-grid-area", el.style.asDynamic().gridRowEnd)
    }

    @Test
    fun gridAreaFourValues() = runTest {
        composition {
            Div({ style { gridArea("2 span", "another-grid-area span", "span 3", "2 span") } })
        }

        val el = nextChild()
        assertEquals("span 2 / span another-grid-area / span 3 / span 2", el.style.asDynamic().gridArea)
    }

}

class GridTemplateAreasTests {
    @Test
    fun gridTemplateAreas() = runTest {
        composition {
            Div({ style { gridTemplateAreas("head head", "nav main", "nav foot")  } })
        }

        assertEquals("\"head head\" \"nav main\" \"nav foot\"", nextChild().style.asDynamic().gridTemplateAreas)
    }
}

class GridJustifyTests {
    @Test
    fun justifySelf() = runTest {
        composition {
            Div({ style { justifySelf("auto") } })
            Div({ style { justifySelf("normal") } })
            Div({ style { justifySelf("stretch") } })
            Div({ style { justifySelf("center") } })
            Div({ style { justifySelf("start") } })
            Div({ style { justifySelf("end") } })
            Div({ style { justifySelf("flex-start") } })
            Div({ style { justifySelf("flex-end") } })
            Div({ style { justifySelf("self-start") } })
            Div({ style { justifySelf("self-end") } })
            Div({ style { justifySelf("left") } })
            Div({ style { justifySelf("right") } })
            Div({ style { justifySelf("baseline") } })
            Div({ style { justifySelf("safe center") } })
            Div({ style { justifySelf("unsafe center") } })
            Div({ style { justifySelf("inherit") } })
            Div({ style { justifySelf("initial") } })
            Div({ style { justifySelf("revert") } })
            Div({ style { justifySelf("unset") } })
        }

        assertEquals("auto", nextChild().style.asDynamic().justifySelf)
        assertEquals("normal", nextChild().style.asDynamic().justifySelf)
        assertEquals("stretch", nextChild().style.asDynamic().justifySelf)
        assertEquals("center", nextChild().style.asDynamic().justifySelf)
        assertEquals("start", nextChild().style.asDynamic().justifySelf)
        assertEquals("end", nextChild().style.asDynamic().justifySelf)
        assertEquals("flex-start", nextChild().style.asDynamic().justifySelf)
        assertEquals("flex-end", nextChild().style.asDynamic().justifySelf)
        assertEquals("self-start", nextChild().style.asDynamic().justifySelf)
        assertEquals("self-end", nextChild().style.asDynamic().justifySelf)
        assertEquals("left", nextChild().style.asDynamic().justifySelf)
        assertEquals("right", nextChild().style.asDynamic().justifySelf)
        assertEquals("baseline", nextChild().style.asDynamic().justifySelf)
        assertEquals("safe center", nextChild().style.asDynamic().justifySelf)
        assertEquals("unsafe center", nextChild().style.asDynamic().justifySelf)
        assertEquals("inherit", nextChild().style.asDynamic().justifySelf)
        assertEquals("initial", nextChild().style.asDynamic().justifySelf)
        assertEquals("revert", nextChild().style.asDynamic().justifySelf)
        assertEquals("unset", nextChild().style.asDynamic().justifySelf)
    }

    @Test
    fun justifyItems() = runTest {
        composition {
            Div({ style { justifyItems("normal") } })
            Div({ style { justifyItems("stretch") } })
            Div({ style { justifyItems("center") } })
            Div({ style { justifyItems("start") } })
            Div({ style { justifyItems("end") } })
            Div({ style { justifyItems("flex-start") } })
            Div({ style { justifyItems("flex-end") } })
            Div({ style { justifyItems("self-start") } })
            Div({ style { justifyItems("self-end") } })
            Div({ style { justifyItems("left") } })
            Div({ style { justifyItems("right") } })
            Div({ style { justifyItems("baseline") } })
            Div({ style { justifyItems("safe center") } })
            Div({ style { justifyItems("unsafe center") } })
            Div({ style { justifyItems("inherit") } })
            Div({ style { justifyItems("initial") } })
            Div({ style { justifyItems("revert") } })
            Div({ style { justifyItems("unset") } })
        }

        assertEquals("normal", nextChild().style.asDynamic().justifyItems)
        assertEquals("stretch", nextChild().style.asDynamic().justifyItems)
        assertEquals("center", nextChild().style.asDynamic().justifyItems)
        assertEquals("start", nextChild().style.asDynamic().justifyItems)
        assertEquals("end", nextChild().style.asDynamic().justifyItems)
        assertEquals("flex-start", nextChild().style.asDynamic().justifyItems)
        assertEquals("flex-end", nextChild().style.asDynamic().justifyItems)
        assertEquals("self-start", nextChild().style.asDynamic().justifyItems)
        assertEquals("self-end", nextChild().style.asDynamic().justifyItems)
        assertEquals("left", nextChild().style.asDynamic().justifyItems)
        assertEquals("right", nextChild().style.asDynamic().justifyItems)
        assertEquals("baseline", nextChild().style.asDynamic().justifyItems)
        assertEquals("safe center", nextChild().style.asDynamic().justifyItems)
        assertEquals("unsafe center", nextChild().style.asDynamic().justifyItems)
        assertEquals("inherit", nextChild().style.asDynamic().justifyItems)
        assertEquals("initial", nextChild().style.asDynamic().justifyItems)
        assertEquals("revert", nextChild().style.asDynamic().justifyItems)
        assertEquals("unset", nextChild().style.asDynamic().justifyItems)
    }
}


class GridAlignSelfTests {
    @Test
    fun alignSelf() = runTest {
        composition {
            Div({ style { alignSelf("auto") } })
            Div({ style { alignSelf("normal") } })
            Div({ style { alignSelf("stretch") } })
            Div({ style { alignSelf("center") } })
            Div({ style { alignSelf("start") } })
            Div({ style { alignSelf("end") } })
            Div({ style { alignSelf("flex-start") } })
            Div({ style { alignSelf("flex-end") } })
            Div({ style { alignSelf("self-start") } })
            Div({ style { alignSelf("self-end") } })
            Div({ style { alignSelf("baseline") } })
            Div({ style { alignSelf("safe center") } })
            Div({ style { alignSelf("unsafe center") } })
            Div({ style { alignSelf("inherit") } })
            Div({ style { alignSelf("initial") } })
            Div({ style { alignSelf("revert") } })
            Div({ style { alignSelf("unset") } })
        }

        assertEquals("auto", nextChild().style.asDynamic().alignSelf)
        assertEquals("normal", nextChild().style.asDynamic().alignSelf)
        assertEquals("stretch", nextChild().style.asDynamic().alignSelf)
        assertEquals("center", nextChild().style.asDynamic().alignSelf)
        assertEquals("start", nextChild().style.asDynamic().alignSelf)
        assertEquals("end", nextChild().style.asDynamic().alignSelf)
        assertEquals("flex-start", nextChild().style.asDynamic().alignSelf)
        assertEquals("flex-end", nextChild().style.asDynamic().alignSelf)
        assertEquals("self-start", nextChild().style.asDynamic().alignSelf)
        assertEquals("self-end", nextChild().style.asDynamic().alignSelf)
        assertEquals("baseline", nextChild().style.asDynamic().alignSelf)
        assertEquals("safe center", nextChild().style.asDynamic().alignSelf)
        assertEquals("unsafe center", nextChild().style.asDynamic().alignSelf)
        assertEquals("inherit", nextChild().style.asDynamic().alignSelf)
        assertEquals("initial", nextChild().style.asDynamic().alignSelf)
        assertEquals("revert", nextChild().style.asDynamic().alignSelf)
        assertEquals("unset", nextChild().style.asDynamic().alignSelf)
    }

    @Test
    fun alignItems() = runTest {
        composition {
            Div({ style { alignItems("normal") } })
            Div({ style { alignItems("stretch") } })
            Div({ style { alignItems("center") } })
            Div({ style { alignItems("start") } })
            Div({ style { alignItems("end") } })
            Div({ style { alignItems("flex-start") } })
            Div({ style { alignItems("flex-end") } })
            Div({ style { alignItems("self-start") } })
            Div({ style { alignItems("self-end") } })
            Div({ style { alignItems("baseline") } })
            Div({ style { alignItems("safe center") } })
            Div({ style { alignItems("unsafe center") } })
            Div({ style { alignItems("inherit") } })
            Div({ style { alignItems("initial") } })
            Div({ style { alignItems("revert") } })
            Div({ style { alignItems("unset") } })
        }

        assertEquals("normal", nextChild().style.asDynamic().alignItems)
        assertEquals("stretch", nextChild().style.asDynamic().alignItems)
        assertEquals("center", nextChild().style.asDynamic().alignItems)
        assertEquals("start", nextChild().style.asDynamic().alignItems)
        assertEquals("end", nextChild().style.asDynamic().alignItems)
        assertEquals("flex-start", nextChild().style.asDynamic().alignItems)
        assertEquals("flex-end", nextChild().style.asDynamic().alignItems)
        assertEquals("self-start", nextChild().style.asDynamic().alignItems)
        assertEquals("self-end", nextChild().style.asDynamic().alignItems)
        assertEquals("baseline", nextChild().style.asDynamic().alignItems)
        assertEquals("safe center", nextChild().style.asDynamic().alignItems)
        assertEquals("unsafe center", nextChild().style.asDynamic().alignItems)
        assertEquals("inherit", nextChild().style.asDynamic().alignItems)
        assertEquals("initial", nextChild().style.asDynamic().alignItems)
        assertEquals("revert", nextChild().style.asDynamic().alignItems)
        assertEquals("unset", nextChild().style.asDynamic().alignItems)
    }

}

class GridGapTests {
    @Test
    fun rowGap() = runTest {
        composition {
            Div({ style { rowGap(20.px) } })
            Div({ style { rowGap(1.em) } })
            Div({ style { rowGap(3.vmin) } })
            Div({ style { rowGap(0.5.cm) } })
            Div({ style { rowGap(10.percent) } })
        }

        assertEquals("20px", nextChild().style.asDynamic().rowGap)
        assertEquals("1em", nextChild().style.asDynamic().rowGap)
        assertEquals("3vmin", nextChild().style.asDynamic().rowGap)
        assertEquals("0.5cm", nextChild().style.asDynamic().rowGap)
        assertEquals("10%", nextChild().style.asDynamic().rowGap)
    }

    @Test
    fun columnGap() = runTest {
        composition {
            Div({ style { columnGap(20.px) } })
            Div({ style { columnGap(1.em) } })
            Div({ style { columnGap(3.vmin) } })
            Div({ style { columnGap(0.5.cm) } })
            Div({ style { columnGap(10.percent) } })
        }

        assertEquals("20px", nextChild().style.asDynamic().columnGap)
        assertEquals("1em", nextChild().style.asDynamic().columnGap)
        assertEquals("3vmin", nextChild().style.asDynamic().columnGap)
        assertEquals("0.5cm", nextChild().style.asDynamic().columnGap)
        assertEquals("10%", nextChild().style.asDynamic().columnGap)
    }

    @Test
    fun gapOneValue() = runTest {
        composition {
            Div({ style { gap(45.px) } })
        }

        val elStyle = nextChild().style.asDynamic()
        assertEquals("45px", elStyle.rowGap)
        assertEquals("45px", elStyle.columnGap)
    }

    @Test
    fun gapTwoValues() = runTest {
        composition {
            Div({ style { gap(20.px, 30.percent) } })
        }

        assertEquals("20px 30%", nextChild().style.asDynamic().gap)
    }

}

class GridAutoFlowTests {
    @Test
    fun gridAutoFlowKeywords() = runTest {
        composition {
            Div({ style { gridAutoFlow(GridAutoFlow.Column) } })
            Div({ style { gridAutoFlow(GridAutoFlow.ColumnDense) } })
            Div({ style { gridAutoFlow(GridAutoFlow.Dense) } })
            Div({ style { gridAutoFlow(GridAutoFlow.Row) } })
        }

        assertEquals("column", nextChild().style.asDynamic().gridAutoFlow)
        assertEquals("column dense", nextChild().style.asDynamic().gridAutoFlow)
        assertEquals("dense", nextChild().style.asDynamic().gridAutoFlow)
        assertEquals("row", nextChild().style.asDynamic().gridAutoFlow)
    }
}
