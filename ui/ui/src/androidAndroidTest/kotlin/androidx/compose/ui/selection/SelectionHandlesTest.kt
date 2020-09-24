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

package androidx.compose.ui.selection

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.runOnUiThreadIR
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.waitAndScreenShot
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(InternalTextApi::class)
class SelectionHandlesTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    private val HANDLE_COLOR = Color(0xFF2B28F5.toInt())
    // Due to the rendering effect of captured bitmap from activity, if we want the pixels from the
    // corners, we need a little bit offset from the edges of the bitmap.
    private val OFFSET_FROM_EDGE = 5

    private val selectionLtrHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        handlesCrossed = false
    )
    private val selectionRtlHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        handlesCrossed = true
    )

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_left_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = true,
                    directions = Pair(
                        selectionLtrHandleDirection.start.direction,
                        selectionLtrHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionLtrHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isNotEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_right_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = true,
                    directions = Pair(
                        selectionRtlHandleDirection.start.direction,
                        selectionRtlHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionRtlHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_right_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = false,
                    directions = Pair(
                        selectionLtrHandleDirection.start.direction,
                        selectionLtrHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionLtrHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_left_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                DefaultSelectionHandle(
                    modifier = Modifier,
                    isStartHandle = false,
                    directions = Pair(
                        selectionRtlHandleDirection.start.direction,
                        selectionRtlHandleDirection.end.direction
                    ),
                    handlesCrossed = selectionRtlHandleDirection.handlesCrossed
                )
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isNotEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    fun isHandleLtrDirection_ltr_handles_not_cross_return_true() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Ltr, areHandlesCrossed = false)
        ).isTrue()
    }

    @Test
    fun isHandleLtrDirection_ltr_handles_cross_return_false() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Ltr, areHandlesCrossed = true)
        ).isFalse()
    }

    @Test
    fun isHandleLtrDirection_rtl_handles_not_cross_return_false() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Rtl, areHandlesCrossed = false)
        ).isFalse()
    }

    @Test
    fun isHandleLtrDirection_rtl_handles_cross_return_true() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Rtl, areHandlesCrossed = true)
        ).isTrue()
    }
}
