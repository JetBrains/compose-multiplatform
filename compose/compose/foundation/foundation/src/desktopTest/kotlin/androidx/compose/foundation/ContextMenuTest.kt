/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test


class ContextMenuTest {

    @get:Rule
    val rule = createComposeRule()

    // https://github.com/JetBrains/compose-multiplatform/issues/2729
    @Test
    fun `contextMenuArea emits one child when open`() {
        var childrenCount = 0

        rule.setContent {
            // We can't just look up the number of children via the semantic node tree because
            // the layout added for the context menu (the empty one in PopupLayout) is not a
            // semantic node
            Layout(
                content = {
                    val state = ContextMenuState()
                    state.status = ContextMenuState.Status.Open(
                        Rect(Offset(1f, 1f), 0f)
                    )

                    ContextMenuArea(
                        items = {
                            listOf(
                                ContextMenuItem(
                                    label = "Copy",
                                    onClick = {}
                                )
                            )
                        },
                        state = state
                    ){
                        Box(content = {})
                    }
                },
                measurePolicy = { measurables, _ ->
                    childrenCount = measurables.size
                    layout(0, 0){}
                }
            )
        }

        assertEquals(1, childrenCount)
    }

}