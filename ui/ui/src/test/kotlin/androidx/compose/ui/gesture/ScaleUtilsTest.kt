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

package androidx.compose.ui.gesture

import androidx.compose.ui.input.pointer.down
import androidx.compose.ui.input.pointer.moveTo
import androidx.compose.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScaleUtilsTest {

    @Test
    fun calculateScaleDifference_1PointerMovesOutX_isCorrect() {
        calculateScaleDifference_2Pointers(
            -1f, 0f,
            0f, 0f,
            -1f, 0f,
            10f, 0f,
            5f
        )
    }

    @Test
    fun calculateScaleDifference_1PointerMovesOutY_isCorrect() {
        calculateScaleDifference_2Pointers(
            0f, -1f,
            0f, 0f,
            0f, -1f,
            0f, 10f,
            5f
        )
    }

    @Test
    fun calculateScaleDifference_1PointerMovesInX_isCorrect() {
        calculateScaleDifference_2Pointers(
            -1f, 0f,
            10f, 0f,
            -1f, 0f,
            0f, 0f,
            -5f
        )
    }

    @Test
    fun calculateScaleDifference_1PointerMovesInY_isCorrect() {
        calculateScaleDifference_2Pointers(
            0f, -1f,
            0f, 10f,
            0f, -1f,
            0f, 0f,
            -5f
        )
    }

    @Test
    fun calculateScaleDifference_2PointersMoveOutX_isCorrect() {
        calculateScaleDifference_2Pointers(
            -.5f, 0f,
            .5f, 0f,
            -5.5f, 0f,
            5.5f, 0f,
            5f
        )
    }

    @Test
    fun calculateScaleDifference_2PointersMoveOutY_isCorrect() {
        calculateScaleDifference_2Pointers(
            0f, -.5f,
            0f, .5f,
            0f, -5.5f,
            0f, 5.5f,
            5f
        )
    }

    @Test
    fun calculateScaleDifference_2PointersMoveInX_isCorrect() {
        calculateScaleDifference_2Pointers(
            -5.5f, 0f,
            5.5f, 0f,
            -.5f, 0f,
            .5f, 0f,
            -5f
        )
    }

    @Test
    fun calculateScaleDifference_2PointersMoveInY_isCorrect() {
        calculateScaleDifference_2Pointers(
            0f, -5.5f,
            0f, 5.5f,
            0f, -.5f,
            0f, .5f,
            -5f
        )
    }

    @Test
    fun calculateScaleDifference_1PointerMoveOutXAndY_isCorrect() {
        calculateScaleDifference_2Pointers(
            0f, 0f,
            3f, 4f,
            0f, 0f,
            6f, 8f,
            2.5f
        )
    }

    @Test
    fun calculateScaleDifference_1PointerMoveInXAndY_isCorrect() {
        calculateScaleDifference_2Pointers(
            0f, 0f,
            6f, 8f,
            0f, 0f,
            3f, 4f,
            -2.5f
        )
    }

    @Test
    fun calculateScaleDifference_2PointerMoveOutXAndY_isCorrect() {
        calculateScaleDifference_2Pointers(
            -3f, -4f,
            3f, 4f,
            -6f, -8f,
            6f, 8f,
            5f
        )
    }

    @Test
    fun calculateScaleDifference_2PointerMoveInXAndY_isCorrect() {
        calculateScaleDifference_2Pointers(
            -6f, -8f,
            6f, 8f,
            -3f, -4f,
            3f, 4f,
            -5f
        )
    }

    private fun calculateScaleDifference_2Pointers(
        x1s: Float,
        y1s: Float,
        x2s: Float,
        y2s: Float,
        x1e: Float,
        y1e: Float,
        x2e: Float,
        y2e: Float,
        expected: Float
    ) {
        val scaleDifference =
            listOf(
                down(0, 0.milliseconds, x1s, y1s)
                    .moveTo(10.milliseconds, x1e, y1e),
                down(1, 0.milliseconds, x2s, y2s)
                    .moveTo(10.milliseconds, x2e, y2e)
            ).calculateAllDimensionInformation().calculateScaleDifference()

        assertThat(scaleDifference).isEqualTo(expected)
    }
}
