/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSDisplayTests {

    @Test
    fun stylesDisplay() = runTest {

        composition {
            Div({ style { display.block() } })
            Div({ style { display.contents() } })
            Div({ style { display.flex() } })
            Div({ style { display.grid() } })
            Div({ style { display.inherit() } })
            Div({ style { display.initial() } })
            Div({ style { display.inlineBlock() } })
            Div({ style { display.inline() } })
            Div({ style { display.legacyFlowRoot() } })
            Div({ style { display.legacyInlineFlex() } })
            Div({ style { display.legacyInlineGrid() } })
            Div({ style { display.listItem() } })
            Div({ style { display.none() } })
            Div({ style { display.table() } })
            Div({ style { display.tableRow() } })
            Div({ style { display.unset() } })
        }

        assertEquals("block", (root.children[0] as HTMLElement).style.display)
        assertEquals("contents", (root.children[1] as HTMLElement).style.display)
        assertEquals("flex", (root.children[2] as HTMLElement).style.display)
        assertEquals("grid", (root.children[3] as HTMLElement).style.display)
        assertEquals("inherit", (root.children[4] as HTMLElement).style.display)
        assertEquals("initial", (root.children[5] as HTMLElement).style.display)
        assertEquals("inline-block", (root.children[6] as HTMLElement).style.display)
        assertEquals("inline", (root.children[7] as HTMLElement).style.display)
        assertEquals("flow-root", (root.children[8] as HTMLElement).style.display)
        assertEquals("inline-flex", (root.children[9] as HTMLElement).style.display)
        assertEquals("inline-grid", (root.children[10] as HTMLElement).style.display)
        assertEquals("list-item", (root.children[11] as HTMLElement).style.display)
        assertEquals("none", (root.children[12] as HTMLElement).style.display)
        assertEquals("table", (root.children[13] as HTMLElement).style.display)
        assertEquals("table-row", (root.children[14] as HTMLElement).style.display)
        assertEquals("unset", (root.children[15] as HTMLElement).style.display)
    }

}