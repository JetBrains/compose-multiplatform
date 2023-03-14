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
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Ignore
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
    private val tagTopLeft = "TopLeft"
    private val tagTopRight = "TopRight"
    private val tagBottomLeft = "BottomLeft"
    private val tagBottomRight = "BottomRight"

    private val colorTopLeft = Color.Red
    private val colorTopRight = Color.Blue
    private val colorBottomLeft = Color.Green
    private val colorBottomRight = Color.Yellow
    private val colorBg = Color.Black

    @Test
    fun captureIndividualRects_checkSizeAndColors() {
        composeCheckerboard()

        var calledCount = 0
        rule.onNodeWithTag(tagTopLeft)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                calledCount++
                colorTopLeft
            }
        assertThat(calledCount).isEqualTo((100 * 50))

        rule.onNodeWithTag(tagTopRight)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                colorTopRight
            }
        rule.onNodeWithTag(tagBottomLeft)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                colorBottomLeft
            }
        rule.onNodeWithTag(tagBottomRight)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(100, 50)) {
                colorBottomRight
            }
    }

    @Test
    fun captureRootContainer_checkSizeAndColors() {
        composeCheckerboard()

        rule.onNodeWithTag(rootTag)
            .captureToImage()
            .assertPixels(expectedSize = IntSize(200, 100)) {
                expectedColorProvider(it)
            }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // b/163023027
    fun captureDialog_verifyBackground() {
        // Test that we are really able to capture dialogs to bitmap.
        setContent {
            AlertDialog(onDismissRequest = {}, confirmButton = {}, backgroundColor = Color.Red)
        }

        rule.onNode(isDialog())
            .captureToImage()
            .assertContainsColor(Color.Red)
    }

    @Ignore // b/266737024
    @Test
    fun capturePopup_verifyBackground() {
        setContent {
            Box {
                Popup {
                    Box(Modifier.background(Color.Red)) {
                        Text("Hello")
                    }
                }
            }
        }

        rule.onNode(isPopup())
            .captureToImage()
            .assertContainsColor(Color.Red)
    }

    @Test
    fun captureComposable_withPopUp_verifyBackground() {
        setContent {
            Box(
                Modifier
                    .testTag(rootTag)
                    .size(300.dp)
                    .background(Color.Yellow)
            ) {
                Popup {
                    Box(Modifier.background(Color.Red)) {
                        Text("Hello")
                    }
                }
            }
        }

        rule.onNodeWithTag(rootTag)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    fun captureComposable_withDialog_verifyBackground() {
        setContent {
            Box(
                Modifier
                    .testTag(rootTag)
                    .size(300.dp)
                    .background(Color.Yellow)
            ) {
                Dialog({}) {
                    Box(
                        Modifier
                            .size(300.dp)
                            .background(Color.Red)) {
                        Text("Hello")
                    }
                }
            }
        }
        rule.onNodeWithTag(rootTag)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    fun capturePopup_verifySize() {
        val boxSize = 200.dp
        val boxSizePx = boxSize.toPixel(rule.density).roundToInt()
        setContent {
            Box {
                Popup {
                    Box(Modifier.size(boxSize)) {
                        Text("Hello")
                    }
                }
            }
        }

        rule.onNode(isPopup())
            .captureToImage()
            .let {
                assertThat(IntSize(it.width, it.height)).isEqualTo(IntSize(boxSizePx, boxSizePx))
            }
    }

    private fun Dp.toPixel(density: Density) = this.value * density.density

    private fun expectedColorProvider(pos: IntOffset): Color {
        if (pos.y < 50) {
            if (pos.x < 100) {
                return colorTopLeft
            } else if (pos.x < 200) {
                return colorTopRight
            }
        } else if (pos.y < 100) {
            if (pos.x < 100) {
                return colorBottomLeft
            } else if (pos.x < 200) {
                return colorBottomRight
            }
        }
        throw IllegalArgumentException("Expected color undefined for position $pos")
    }

    private fun composeCheckerboard() {
        with(rule.density) {
            setContent {
                Box(Modifier.background(colorBg)) {
                    Box(
                        Modifier
                            .padding(top = 20.toDp())
                            .background(colorBg)) {
                        Column(Modifier.testTag(rootTag)) {
                            Row {
                                Box(
                                    Modifier
                                        .testTag(tagTopLeft)
                                        .size(100.toDp(), 50.toDp())
                                        .background(color = colorTopLeft)
                                )
                                Box(
                                    Modifier
                                        .testTag(tagTopRight)
                                        .size(100.toDp(), 50.toDp())
                                        .background(colorTopRight)
                                )
                            }
                            Row {
                                Box(
                                    Modifier
                                        .testTag(tagBottomLeft)
                                        .size(100.toDp(), 50.toDp())
                                        .background(colorBottomLeft)
                                )
                                Box(
                                    Modifier
                                        .testTag(tagBottomRight)
                                        .size(100.toDp(), 50.toDp())
                                        .background(colorBottomRight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        when (val activity = rule.activity) {
            is ActivityWithActionBar -> activity.setContent(content)
            else -> rule.setContent(content)
        }
    }

    private fun fetchNodeRootView(nodeTag: String): View {
        return fetchNodeInteraction(nodeTag).fetchRootView()
    }

    private fun fetchNodeInteraction(nodeTag: String): SemanticsNodeInteraction {
        return rule.onNodeWithTag(nodeTag)
    }
}

private fun SemanticsNodeInteraction.fetchRootView(): View {
    val node = fetchSemanticsNode()
    return (node.root as ViewRootForTest).view
}
