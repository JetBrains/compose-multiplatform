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

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import kotlin.math.min
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AlignmentLineTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun queryingLinesOfUnmeasuredChild() {
        val root = root {
            queryAlignmentLineDuringMeasure()
            add(
                node {
                    doNotMeasure()
                    add(node())
                }
            )
        }

        createDelegate(root)
        assertMeasuredAndLaidOut(root)
    }

    @Test
    fun alignmentLinesPositionInCooperation_whenModifierDisobeys() {
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        val hLinePosition = 50
        val vLinePosition = 150
        val constrainedSize = 100
        val actualSize = 200
        rule.setContent {
            val contentWithAlignmentLines = @Composable {
                Box(Modifier.requiredSize(with(rule.density) { actualSize.toDp() })) {
                    Layout({}, Modifier) { _, _ ->
                        layout(0, 0, mapOf(hLine to hLinePosition, vLine to vLinePosition)) {}
                    }
                }
            }
            Layout(contentWithAlignmentLines) { measurables, _ ->
                val placeable = measurables.first().measure(
                    Constraints(maxWidth = constrainedSize, maxHeight = constrainedSize)
                )
                val obtainedHLinePosition = placeable[hLine]
                val obtainedVLinePosition = placeable[vLine]
                assertEquals(
                    hLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedHLinePosition
                )
                assertEquals(
                    vLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedVLinePosition
                )
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun alignmentLinesPositionInCooperation_whenLayoutDisobeys() {
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        val hLinePosition = 50
        val vLinePosition = 150
        val constrainedSize = 100
        val actualSize = 200
        rule.setContent {
            val contentWithAlignmentLines = @Composable {
                Layout({}, Modifier) { _, _ ->
                    layout(
                        actualSize,
                        actualSize,
                        mapOf(hLine to hLinePosition, vLine to vLinePosition)
                    ) {}
                }
            }
            Layout(contentWithAlignmentLines) { measurables, _ ->
                val placeable = measurables.first().measure(
                    Constraints(maxWidth = constrainedSize, maxHeight = constrainedSize)
                )
                val obtainedHLinePosition = placeable[hLine]
                val obtainedVLinePosition = placeable[vLine]
                assertEquals(
                    hLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedHLinePosition
                )
                assertEquals(
                    vLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedVLinePosition
                )
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }
}