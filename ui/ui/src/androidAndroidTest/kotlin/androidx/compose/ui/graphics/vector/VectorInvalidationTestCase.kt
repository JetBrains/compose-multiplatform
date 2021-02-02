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

package androidx.compose.ui.graphics.vector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.R

class VectorInvalidationTestCase() {

    // Lazily initialize state as it needs to be constructed in the composition
    private var vectorState: MutableState<Int>? = null

    internal var measured: Boolean = false

    /**
     * Queries the size of the underlying vector image to draw
     * This assumes both width and height are the same
     */
    var vectorSize: Int = 0

    @Composable
    fun TestVector() {
        val state = remember { mutableStateOf(R.drawable.ic_triangle2) }
        vectorState = state

        val imageVector = painterResource(state.value)
        with(LocalDensity.current) {
            val width = imageVector.intrinsicSize.width.toInt()
            vectorSize = width
            AtLeastSize(
                size = width,
                modifier = WhiteBackground.paint(imageVector)
            ) {
                measured = true
            }
        }
    }

    val WhiteBackground = Modifier.drawBehind {
        drawRect(Color.White)
    }

    fun toggle() {
        val state = vectorState
        if (state != null) {
            state.value = if (state.value == R.drawable.ic_triangle) {
                R.drawable.ic_triangle2
            } else {
                R.drawable.ic_triangle
            }
        }
    }
}