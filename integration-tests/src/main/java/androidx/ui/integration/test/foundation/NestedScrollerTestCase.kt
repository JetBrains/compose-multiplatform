/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.integration.test.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.ui.test.ToggleableTestCase
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.ui.test.ComposeTestCase
import kotlin.random.Random

/**
 * Test case that puts many horizontal scrollers in a vertical scroller
 */
class NestedScrollerTestCase : ComposeTestCase, ToggleableTestCase {
    // ScrollerPosition must now be constructed during composition to obtain the Density
    private lateinit var scrollState: ScrollState

    @Composable
    override fun emitContent() {
        scrollState = rememberScrollState()
        MaterialTheme {
            Surface {
                ScrollableColumn {
                    repeat(5) { index ->
                        // key is needed because of b/154920561
                        key(index) {
                            SquareRow(index == 0)
                        }
                    }
                }
            }
        }
    }

    override fun toggleState() {
        scrollState.scrollTo(if (scrollState.value == 0f) 10f else 0f)
    }

    @Composable
    fun SquareRow(useScrollerPosition: Boolean) {
        val playStoreColor = Color(red = 0x00, green = 0x00, blue = 0x80)
        val content: @Composable RowScope.() -> Unit = {
            repeat(6) {
                with(DensityAmbient.current) {
                    Column(Modifier.fillMaxHeight()) {
                        val color = remember {
                            val red = Random.nextInt(256)
                            val green = Random.nextInt(256)
                            val blue = Random.nextInt(256)
                            Color(red = red, green = green, blue = blue)
                        }
                        Box(Modifier.preferredSize(350f.toDp()).background(color = color))
                        Text(
                            text = "Some title",
                            color = Color.Black,
                            fontSize = 60f.toSp()
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "3.5 ★",
                                fontSize = 40.toSp(),
                                modifier = Modifier.gravity(Alignment.CenterVertically)
                            )
                            Box(
                                Modifier
                                    .gravity(Alignment.CenterVertically)
                                    .preferredSize(40f.toDp())
                                    .background(color = playStoreColor)
                            )
                        }
                    }
                }
            }
        }
        if (useScrollerPosition) {
            ScrollableRow(scrollState = scrollState, children = content)
        } else {
            ScrollableRow(children = content)
        }
    }
}
