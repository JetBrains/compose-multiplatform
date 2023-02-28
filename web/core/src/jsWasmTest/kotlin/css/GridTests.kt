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

external interface GridStyleExt {
    val gridColumnStart: String
    val gridColumnEnd: String
    val gridRowStart: String
    val gridRowEnd: String
    val gridTemplateRows: String
    val gridAutoRows: String
    val gridTemplateColumns: String
    val gridAutoColumns: String
    val gridTemplateAreas: String
    val justifySelf: String
    val justifyItems: String
    val alignSelf: String
    val alignItems: String
    val columnGap: String
    val gridAutoFlow: String
    val gap: String
    val rowGap: String
    val gridArea: String
}

@OptIn(ComposeWebExperimentalTestsApi::class)
class GridColumnTests {
    @Test
    fun gridColumnOneValue() = runTest {
        composition {
            Div({ style { gridColumn("1") } })
        }

        val el = nextChild().style as GridStyleExt
        assertEquals("1", el.gridColumnStart)
        assertEquals("auto", el.gridColumnEnd)
    }

    @Test
    fun gridColumnTwoValues() = runTest {
        composition {
            Div({ style { gridColumn(1, 3) } })
        }

        val el = nextChild().style as GridStyleExt
        assertEquals("1", el.gridColumnStart)
        assertEquals("3", el.gridColumnEnd)
    }

    @Test
    fun gridColumnLineNames() = runTest {
        composition {
            Div({ style { gridColumn("main-start") } })
            Div({ style { gridColumn("main-start", "main-end") } })
        }

        assertEquals("main-start", (nextChild().style as GridStyleExt).gridColumnStart)

        nextChild().style.apply {
            assertEquals("main-start", (this as GridStyleExt).gridColumnStart)
            assertEquals("main-end", (this as GridStyleExt).gridColumnEnd)
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridColumnStart)
    }
}

class GridColumnEndTests {
    @Test
    fun gridColumnEndOneValue() = runTest {
        composition {
            Div({ style { gridColumnEnd("1") } })
            Div({ style { gridColumnEnd("somegridarea") } })
        }

        assertEquals("1", (nextChild().style as GridStyleExt).gridColumnEnd)
        assertEquals("somegridarea", (nextChild().style as GridStyleExt).gridColumnEnd)
    }

    @Test
    fun gridColumnEndIntValue() = runTest {
        composition {
            Div({ style { gridColumnEnd(-4) } })
        }

        val el = nextChild().style as GridStyleExt
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridColumnEnd)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridColumnEnd)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridColumnEnd)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridColumnEnd)
    }
}

class GridColumnStartTests {
    @Test
    fun gridColumnStartOneValue() = runTest {
        composition {
            Div({ style { gridColumnStart("1") } })
            Div({ style { gridColumnStart("somegridarea") } })
        }

        assertEquals("1", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("somegridarea", (nextChild().style as GridStyleExt).gridColumnStart)
    }

    @Test
    fun gridColumnStartIntValue() = runTest {
        composition {
            Div({ style { gridColumnStart(-4) } })
        }

        val el = nextChild().style as GridStyleExt
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridColumnStart)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridColumnStart)
    }
}

class GridRowStartTests {
    @Test
    fun gridRowStartOneValue() = runTest {
        composition {
            Div({ style { gridRowStart("1") } })
            Div({ style { gridRowStart("somegridarea") } })
        }

        assertEquals("1", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("somegridarea", (nextChild().style as GridStyleExt).gridRowStart)
    }

    @Test
    fun gridRowStartIntValue() = runTest {
        composition {
            Div({ style { gridRowStart(-4) } })
        }

        val el = nextChild().style as GridStyleExt
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridRowStart)
    }
}

class GridRowEndTests {
    @Test
    fun gridRowEndOneValue() = runTest {
        composition {
            Div({ style { gridRowEnd("1") } })
            Div({ style { gridRowEnd("somegridarea") } })
        }

        assertEquals("1", (nextChild().style as GridStyleExt).gridRowEnd)
        assertEquals("somegridarea", (nextChild().style as GridStyleExt).gridRowEnd)
    }

    @Test
    fun gridRowEndIntValue() = runTest {
        composition {
            Div({ style { gridRowEnd(-4) } })
        }

        val el = nextChild().style as GridStyleExt
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridRowEnd)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridRowEnd)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridRowEnd)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridRowEnd)
    }
}


class GridRawTests {

    @Test
    fun gridRowOneValue() = runTest {
        composition {
            Div({ style { gridRow("1") } })
        }

        val el = nextChild().style as GridStyleExt
        assertEquals("1", el.gridRowStart)
        assertEquals("auto", el.gridRowEnd)
    }

    @Test
    fun gridRowTwoValues() = runTest {
        composition {
            Div({ style { gridRow(2, -1) } })
        }

        val el = nextChild().style as GridStyleExt
        assertEquals("2", el.gridRowStart)
        assertEquals("-1", el.gridRowEnd)
    }

    @Test
    fun gridRowCustomIndentValuesStrInt() = runTest {
        composition {
            Div({ style { gridRow("4 somegridarea", 6) } })
        }

        val el = nextChild().style as GridStyleExt
        assertEquals("4 somegridarea", el.gridRowStart)
        assertEquals("6", el.gridRowEnd)
    }

    @Test
    fun gridRowCustomIndentValuesIntStr() = runTest {
        composition {
            Div({ style { gridRow(5, "4 somegridarea") } })
        }

        val el = nextChild().style as GridStyleExt
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridRowStart)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridRowStart)
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridTemplateRows)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridTemplateRows)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridTemplateRows)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridTemplateRows)
    }

    @Test
    fun gridAutoRowsGlobalValues() = runTest {
        composition {
            Div({ style { gridAutoRows("inherit") } })
            Div({ style { gridAutoRows("initial") } })
            Div({ style { gridAutoRows("revert") } })
            Div({ style { gridAutoRows("unset") } })
        }

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridAutoRows)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridAutoRows)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridAutoRows)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridAutoRows)
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

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridTemplateColumns)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridTemplateColumns)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridTemplateColumns)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridTemplateColumns)
    }

    @Test
    fun gridAutoColumnsGlobalValues() = runTest {
        composition {
            Div({ style { gridAutoColumns("inherit") } })
            Div({ style { gridAutoColumns("initial") } })
            Div({ style { gridAutoColumns("revert") } })
            Div({ style { gridAutoColumns("unset") } })
        }

        assertEquals("inherit", (nextChild().style as GridStyleExt).gridAutoColumns)
        assertEquals("initial", (nextChild().style as GridStyleExt).gridAutoColumns)
        assertEquals("revert", (nextChild().style as GridStyleExt).gridAutoColumns)
        assertEquals("unset", (nextChild().style as GridStyleExt).gridAutoColumns)
    }
}

class GridAreaTests {
    @Test
    fun gridAreaOneValue() = runTest {
        composition {
            Div({ style { gridArea("span 3") } })
        }

        assertEquals("span 3", (nextChild().style as GridStyleExt).gridRowStart)
    }

    @Test
    fun gridAreaTwoValues() = runTest {
        composition {
            Div({ style { gridArea("some-grid-area", "another-grid-area") } })
        }

        val el = nextChild()
        assertEquals("some-grid-area", (el.style as GridStyleExt).gridRowStart)
        assertEquals("another-grid-area", (el.style as GridStyleExt).gridColumnStart)
    }

    @Test
    fun gridAreaThreeValues() = runTest {
        composition {
            Div({ style { gridArea("some-grid-area", "another-grid-area", "yet-another-grid-area") } })
        }

        val el = nextChild()
        assertEquals("some-grid-area", (el.style as GridStyleExt).gridRowStart)
        assertEquals("another-grid-area", (el.style as GridStyleExt).gridColumnStart)
        assertEquals("yet-another-grid-area", (el.style as GridStyleExt).gridRowEnd)
    }

    @Test
    fun gridAreaFourValues() = runTest {
        composition {
            Div({ style { gridArea("2 span", "another-grid-area span", "span 3", "2 span") } })
        }

        val el = nextChild()
        assertEquals("span 2 / span another-grid-area / span 3 / span 2", (el.style as GridStyleExt).gridArea)
    }

}

class GridTemplateAreasTests {
    @Test
    fun gridTemplateAreas() = runTest {
        composition {
            Div({ style { gridTemplateAreas("head head", "nav main", "nav foot")  } })
        }

        assertEquals("\"head head\" \"nav main\" \"nav foot\"", (nextChild().style as GridStyleExt).gridTemplateAreas)
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

        assertEquals("auto", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("normal", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("stretch", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("center", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("start", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("end", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("flex-start", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("flex-end", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("self-start", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("self-end", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("left", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("right", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("baseline", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("safe center", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("unsafe center", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("inherit", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("initial", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("revert", (nextChild().style as GridStyleExt).justifySelf)
        assertEquals("unset", (nextChild().style as GridStyleExt).justifySelf)
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

        assertEquals("normal", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("stretch", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("center", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("start", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("end", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("flex-start", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("flex-end", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("self-start", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("self-end", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("left", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("right", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("baseline", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("safe center", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("unsafe center", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("inherit", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("initial", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("revert", (nextChild().style as GridStyleExt).justifyItems)
        assertEquals("unset", (nextChild().style as GridStyleExt).justifyItems)
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

        assertEquals("auto", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("normal", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("stretch", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("center", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("start", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("end", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("flex-start", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("flex-end", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("self-start", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("self-end", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("baseline", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("safe center", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("unsafe center", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("inherit", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("initial", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("revert", (nextChild().style as GridStyleExt).alignSelf)
        assertEquals("unset", (nextChild().style as GridStyleExt).alignSelf)
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

        assertEquals("normal", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("stretch", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("center", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("start", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("end", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("flex-start", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("flex-end", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("self-start", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("self-end", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("baseline", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("safe center", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("unsafe center", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("inherit", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("initial", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("revert", (nextChild().style as GridStyleExt).alignItems)
        assertEquals("unset", (nextChild().style as GridStyleExt).alignItems)
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

        assertEquals("20px", (nextChild().style as GridStyleExt).rowGap)
        assertEquals("1em", (nextChild().style as GridStyleExt).rowGap)
        assertEquals("3vmin", (nextChild().style as GridStyleExt).rowGap)
        assertEquals("0.5cm", (nextChild().style as GridStyleExt).rowGap)
        assertEquals("10%", (nextChild().style as GridStyleExt).rowGap)
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

        assertEquals("20px", (nextChild().style as GridStyleExt).columnGap)
        assertEquals("1em", (nextChild().style as GridStyleExt).columnGap)
        assertEquals("3vmin", (nextChild().style as GridStyleExt).columnGap)
        assertEquals("0.5cm", (nextChild().style as GridStyleExt).columnGap)
        assertEquals("10%", (nextChild().style as GridStyleExt).columnGap)
    }

    @Test
    fun gapOneValue() = runTest {
        composition {
            Div({ style { gap(45.px) } })
        }

        val elStyle = (nextChild().style as GridStyleExt)
        assertEquals("45px", elStyle.rowGap)
        assertEquals("45px", elStyle.columnGap)
    }

    @Test
    fun gapTwoValues() = runTest {
        composition {
            Div({ style { gap(20.px, 30.percent) } })
        }

        assertEquals("20px 30%", (nextChild().style as GridStyleExt).gap)
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

        assertEquals("column", (nextChild().style as GridStyleExt).gridAutoFlow)
        assertEquals("column dense", (nextChild().style as GridStyleExt).gridAutoFlow)
        assertEquals("dense", (nextChild().style as GridStyleExt).gridAutoFlow)
        assertEquals("row", (nextChild().style as GridStyleExt).gridAutoFlow)
    }
}
