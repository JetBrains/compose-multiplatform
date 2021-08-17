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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestrictedSizeTest {
    @get:Rule
    val rule = createComposeRule()

    private val tag = "tag"

    @Test
    fun pointerPositionAtMeasuredSize(): Unit = with(rule.density) {
        var point = Offset.Zero

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.requiredSize(50.dp).testTag(tag)) {
                    Box(
                        Modifier.requiredSize(80.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                val event = awaitPointerEvent()
                                point = event.changes[0].position
                            }
                        }
                    )
                }
            }
        }

        rule.onNodeWithTag(tag)
            .performGesture {
                click(Offset.Zero)
            }

        assertThat(point.x).isEqualTo(15.dp.roundToPx().toFloat())
        assertThat(point.y).isEqualTo(15.dp.roundToPx().toFloat())
    }

    @Test
    fun pointerOutOfLayoutBounds(): Unit = with(rule.density) {
        var point = Offset.Zero
        var isOutOfBounds = true

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.requiredSize(50.dp).testTag(tag)) {
                    Box(
                        Modifier.requiredSize(80.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                val event = awaitPointerEvent()
                                point = event.changes[0].position
                                isOutOfBounds =
                                    event.changes[0].isOutOfBounds(size, extendedTouchPadding)
                            }
                        }
                    )
                }
            }
        }

        rule.onNodeWithTag(tag)
            .performGesture {
                click(Offset(-5f, -2f))
            }

        assertThat(point.x).isEqualTo(15.dp.roundToPx().toFloat() - 5f)
        assertThat(point.y).isEqualTo(15.dp.roundToPx().toFloat() - 2f)
        assertThat(isOutOfBounds).isFalse()
    }

    @Test
    fun semanticsSizeTooSmall(): Unit = with(rule.density) {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.requiredSize(50.dp)) {
                    Box(
                        Modifier.requiredSize(80.dp).testTag(tag)
                    )
                }
            }
        }

        rule.onNodeWithTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(80.dp)
    }
}