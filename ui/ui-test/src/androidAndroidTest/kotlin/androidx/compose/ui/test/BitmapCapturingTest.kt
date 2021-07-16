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

package androidx.compose.ui.test

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class BitmapCapturingTest(val config: TestConfig) {
    data class TestConfig(
        val activityClass: Class<out ComponentActivity>
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> = listOf(
            TestConfig(ComponentActivity::class.java),
            TestConfig(ActivityWithActionBar::class.java)
        )
    }

    @get:Rule
    val rule = createAndroidComposeRule(config.activityClass)

    private val rootTag = "Root"
    private val tag11 = "Rect11"
    private val tag12 = "Rect12"
    private val tag21 = "Rect21"
    private val tag22 = "Rect22"

    private val color11 = Color.Red
    private val color12 = Color.Blue
    private val color21 = Color.Green
    private val color22 = Color.Yellow
    private val colorBg = Color.Black

    @Test
    fun captureIndividualRects_checkSizeAndColors() {
        composeCheckerboard()

        var calledCount = 0
        rule.onNodeWithTag(tag11)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                calledCount++
                color11
            }
        assertThat(calledCount).isEqualTo((100 * 50))

        rule.onNodeWithTag(tag12)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color12
            }
        rule.onNodeWithTag(tag21)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color21
            }
        rule.onNodeWithTag(tag22)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color22
            }
    }

    @Test
    fun captureRootContainer_checkSizeAndColors() {
        composeCheckerboard()

        rule.onNodeWithTag(rootTag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(200, 100)) {
                if (it.y >= 100 || it.x >= 200) {
                    throw AssertionError("$it is out of range!")
                }
                expectedColorProvider(it)
            }
    }

    @Test(expected = AssertionError::class)
    fun assertWrongColor_expectException() {
        composeCheckerboard()

        rule.onNodeWithTag(tag11)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                color22 // Assuming wrong color
            }
    }

    @Test(expected = AssertionError::class)
    fun assertWrongSize_expectException() {
        composeCheckerboard()

        rule.onNodeWithTag(tag11)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(10, 10)) {
                color21
            }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // b/163023027
    fun captureDialog_verifyBackground() {
        // Test that we are really able to capture dialogs to bitmap.
        rule.setContent {
            AlertDialog(onDismissRequest = {}, confirmButton = {}, backgroundColor = Color.Red)
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertContainsColor(Color.Red)
    }

    @Test
    fun capturePopup_shouldFail() {
        // Test that we throw an error when trying to capture a popup.
        rule.setContent {
            Box {
                Popup {
                    Text("Hello")
                }
            }
        }

        expectError<IllegalArgumentException>(
            expectedMessage = ".*Popups currently cannot be captured to bitmap.*"
        ) {
            rule.onNode(isPopup())
                .captureToImage()
        }
    }

    private fun expectedColorProvider(pos: IntOffset): Color {
        if (pos.y < 50) {
            if (pos.x < 100) {
                return color11
            } else if (pos.x < 200) {
                return color12
            }
        } else if (pos.y < 100) {
            if (pos.x < 100) {
                return color21
            } else if (pos.x < 200) {
                return color22
            }
        }
        return colorBg
    }

    private fun composeCheckerboard() {
        with(rule.density) {
            rule.setContent {
                Box(Modifier.fillMaxSize().background(colorBg)) {
                    Box(Modifier.padding(top = 20.toDp()).background(colorBg)) {
                        Column(Modifier.testTag(rootTag)) {
                            Row {
                                Box(
                                    Modifier
                                        .testTag(tag11)
                                        .size(100.toDp(), 50.toDp())
                                        .background(color = color11)
                                )
                                Box(
                                    Modifier
                                        .testTag(tag12)
                                        .size(100.toDp(), 50.toDp())
                                        .background(color12)
                                )
                            }
                            Row {
                                Box(
                                    Modifier
                                        .testTag(tag21)
                                        .size(100.toDp(), 50.toDp())
                                        .background(color21)
                                )
                                Box(
                                    Modifier
                                        .testTag(tag22)
                                        .size(100.toDp(), 50.toDp())
                                        .background(color22)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}