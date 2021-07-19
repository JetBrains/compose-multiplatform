/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSDisplayTests {

    @Test
    fun stylesDisplay() = runTest {
        composition {
            Div({ style { display(Block) } })
            Div({ style { display(Inline) } })
            Div({ style { display(InlineBlock) } })
            Div({ style { display(Flex) } })
            Div({ style { display(LegacyInlineFlex) } })
            Div({ style { display(Grid) } })
            Div({ style { display(LegacyInlineGrid) } })
            Div({ style { display(FlowRoot) } })
            Div({ style { display(None) } })
            Div({ style { display(Contents) } })
            Div({ style { display(Table) } })
            Div({ style { display(TableRow) } })
            Div({ style { display(ListItem) } })
            Div({ style { display(Inherit) } })
            Div({ style { display(Initial) } })
            Div({ style { display(Unset) } })
        }

        var counter = 0
        assertEquals("block", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("inline", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("inline-block", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("flex", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("inline-flex", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("grid", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("inline-grid", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("flow-root", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("none", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("contents", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("table", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("table-row", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("list-item", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("inherit", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("initial", (root.children[counter++] as HTMLElement).style.display)
        assertEquals("unset", (root.children[counter] as HTMLElement).style.display)
    }

}