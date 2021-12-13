/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.layout.Placeable.PlacementScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MeasuringPlacingTwiceIsNotAllowedTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun measureTwiceInMeasureBlock() {
        assertException(measureBlock = { measurable, constraints ->
            measurable.measure(constraints)
            measurable.measure(constraints)
        })
    }

    @Test
    fun measureTwiceInMeasureBlockWithDifferentConstraints() {
        assertException(measureBlock = { measurable, _ ->
            measurable.measure(Constraints.fixed(100, 100))
            measurable.measure(Constraints.fixed(200, 200))
        })
    }

    @Test
    fun measureTwiceInLayoutBlock() {
        assertException(layoutBlock = { measurable, constraints ->
            measurable.measure(constraints)
            measurable.measure(constraints)
        })
    }

    @Test
    fun measureInBothStages() {
        assertException(
            measureBlock = { measurable, constraints ->
                measurable.measure(constraints)
            },
            layoutBlock = { measurable, constraints ->
                measurable.measure(constraints)
            }
        )
    }

    @Test
    fun placeTwiceWithTheSamePosition() {
        assertException(
            layoutBlock = { measurable, constraints ->
                measurable.measure(constraints).also {
                    it.place(0, 0)
                    it.place(0, 0)
                }
            }
        )
    }

    @Test
    fun placeTwiceWithDifferentPositions() {
        assertException(
            layoutBlock = { measurable, constraints ->
                measurable.measure(constraints).also {
                    it.place(0, 0)
                    it.place(10, 10)
                }
            }
        )
    }

    @Test
    fun placeTwiceWithLayer() {
        assertException(
            layoutBlock = { measurable, constraints ->
                measurable.measure(constraints).also {
                    it.placeWithLayer(0, 0)
                    it.placeWithLayer(0, 0)
                }
            }
        )
    }

    private fun assertException(
        measureBlock: (Measurable, Constraints) -> Unit = { _, _ -> },
        layoutBlock: PlacementScope.(Measurable, Constraints) -> Unit = { _, _ -> }
    ) {
        var exception: Exception? = null
        rule.setContent {
            Layout(content = {
                AtLeastSize(50)
            }) { measurables, constraints ->
                try {
                    measureBlock(measurables.first(), constraints)
                } catch (e: Exception) {
                    exception = e
                }
                layout(100, 100) {
                    try {
                        layoutBlock(measurables.first(), constraints)
                    } catch (e: Exception) {
                        exception = e
                    }
                }
            }
        }
        assertThat(exception).isNotNull()
    }
}
