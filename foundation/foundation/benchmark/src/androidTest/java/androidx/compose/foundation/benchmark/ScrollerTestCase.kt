/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking

/**
 * Test case that puts a large number of boxes in a column in a vertical scroller to force scrolling.
 */
class ScrollerTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var scrollState: ScrollState

    @Composable
    override fun MeasuredContent() {
        scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            Column(Modifier.fillMaxHeight()) {
                for (green in 0..0xFF) {
                    ColorStripe(0xFF, green, 0)
                }
                for (red in 0xFF downTo 0) {
                    ColorStripe(red, 0xFF, 0)
                }
                for (blue in 0..0xFF) {
                    ColorStripe(0, 0xFF, blue)
                }
                for (green in 0xFF downTo 0) {
                    ColorStripe(0, green, 0xFF)
                }
                for (red in 0..0xFF) {
                    ColorStripe(red, 0, 0xFF)
                }
                for (blue in 0xFF downTo 0) {
                    ColorStripe(0xFF, 0, blue)
                }
            }
        }
    }

    override fun toggleState() {
        runBlocking { scrollState.scrollTo(if (scrollState.value == 0) 10 else 0) }
    }

    @Composable
    fun ColorStripe(red: Int, green: Int, blue: Int) {
        Canvas(Modifier.size(45.dp, 5.dp)) {
            drawRect(Color(red = red, green = green, blue = blue))
        }
    }
}