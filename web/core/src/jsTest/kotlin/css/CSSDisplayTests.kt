/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.css

import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals

class CSSDisplayTests {

    @Test
    fun stylesDisplay() = runTest {
        val enumValues = listOf(
            DisplayStyle.Block,
            DisplayStyle.Inline,
            DisplayStyle.InlineBlock,
            DisplayStyle.Flex,
            DisplayStyle.LegacyInlineFlex,
            DisplayStyle.Grid,
            DisplayStyle.LegacyInlineGrid,
            DisplayStyle.FlowRoot,
            DisplayStyle.None,
            DisplayStyle.Contents,
            DisplayStyle.Table,
            DisplayStyle.TableRow,
            DisplayStyle.ListItem,
            DisplayStyle.Inherit,
            DisplayStyle.Initial,
            DisplayStyle.Unset
        )

        composition {
            enumValues.forEach { displayStyle ->
                Div(
                    {
                        style {
                            display(displayStyle)
                        }
                    }
                )
            }
        }

        enumValues.forEachIndexed { index, displayStyle ->
            assertEquals(
                displayStyle.toString(),
                (root.children[index] as HTMLElement).style.display
            )
        }
    }

}