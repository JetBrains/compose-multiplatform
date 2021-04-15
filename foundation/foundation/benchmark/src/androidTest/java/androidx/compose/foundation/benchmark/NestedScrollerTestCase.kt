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

package androidx.compose.foundation.benchmark

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/**
 * Test case that puts many horizontal scrollers in a vertical scroller
 */
class NestedScrollerTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    // ScrollerPosition must now be constructed during composition to obtain the Density
    private lateinit var scrollState: ScrollState

    @Composable
    override fun MeasuredContent() {
        scrollState = rememberScrollState()
        LazyColumn {
            items(5) { index ->
                SquareRow(index == 0)
            }
        }
    }

    override fun toggleState() {
        runBlocking { scrollState.scrollTo(if (scrollState.value == 0) 10 else 0) }
    }

    @Composable
    fun SquareRow(useScrollerPosition: Boolean) {
        val playStoreColor = Color(red = 0x00, green = 0x00, blue = 0x80)
        val content: @Composable RowScope.() -> Unit = {
            repeat(6) {
                with(LocalDensity.current) {
                    Column(Modifier.fillMaxHeight()) {
                        val color = remember {
                            val red = Random.nextInt(256)
                            val green = Random.nextInt(256)
                            val blue = Random.nextInt(256)
                            Color(red = red, green = green, blue = blue)
                        }
                        Box(Modifier.size(350f.toDp()).background(color = color))
                        Text(
                            text = "Some title",
                            color = Color.Black,
                            fontSize = 60f.toSp()
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "3.5 ★",
                                fontSize = 40.toSp(),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Box(
                                Modifier
                                    .align(Alignment.CenterVertically)
                                    .size(40f.toDp())
                                    .background(color = playStoreColor)
                            )
                        }
                    }
                }
            }
        }
        val state = if (useScrollerPosition) scrollState else rememberScrollState()
        Row(Modifier.horizontalScroll(state), content = content)
    }
}