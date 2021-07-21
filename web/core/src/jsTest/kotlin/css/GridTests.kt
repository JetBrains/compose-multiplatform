/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
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
        assertEquals("main-start", nextChild().style.asDynamic().gridColumnStart)
        assertEquals("main-end", currentChild().style.asDynamic().gridColumnEnd)
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
    fun gridTemplateRawsGlobalValues() = runTest {
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
