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
import android.view.WindowManager
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
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.expectError
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
import androidx.test.filters.FlakyTest
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
    fun captureIndividualRects_checkSizeAndColors_multiWindow() {
        composeCheckerboard()

        var calledCount = 0
        rule.onNodeWithTag(tagTopLeft)
            .captureToImage(useAllWindows = true)
            .assertPixels(expectedSize = IntSize(100, 50)) {
                calledCount++
                colorTopLeft
            }
        assertThat(calledCount).isEqualTo((100 * 50))

        rule.onNodeWithTag(tagTopRight)
            .captureToImage(useAllWindows = true)
            .assertPixels(expectedSize = IntSize(100, 50)) {
                colorTopRight
            }
        rule.onNodeWithTag(tagBottomLeft)
            .captureToImage(useAllWindows = true)
            .assertPixels(expectedSize = IntSize(100, 50)) {
                colorBottomLeft
            }
        rule.onNodeWithTag(tagBottomRight)
            .captureToImage(useAllWindows = true)
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
    @FlakyTest(bugId = 238872517)
    fun captureRootContainer_checkSizeAndColors_multiWindow() {
        composeCheckerboard()

        rule.onNodeWithTag(rootTag)
            .captureToImage(useAllWindows = true)
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

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // b/163023027
    fun captureDialog_verifyBackground_multiWindow() {
        // Test that we are really able to capture dialogs to bitmap.
        setContent {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                modifier = Modifier.size(200.dp),
                backgroundColor = Color.Red
            )
        }

        rule.onNode(isDialog())
            .captureToImage(useAllWindows = true)
            .assertContainsColor(Color.Red)
    }

    @Test
    fun capturePopup_shouldFail() {
        // Test that we throw an error when trying to capture a popup.
        setContent {
            Box {
                Popup {
                    Text("Hello")
                }
            }
        }

        expectError<IllegalArgumentException>(
            expectedMessage = "The node that is being captured " +
                "to bitmap is in a popup or is a popup itself.*"
        ) {
            rule.onNode(isPopup())
                .captureToImage()
        }
    }

    @Ignore("b/235839078")
    @Test
    fun capturePopup_verifyBackground_multiWindow() {
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
            .captureToImage(useAllWindows = true)
            .assertContainsColor(Color.Red)
    }

    @Test
    fun capturePopup_withComposable_verifyBackground_multiWindow() {
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
            .captureToImage(useAllWindows = true)
            .assertContainsColor(Color.Red)
            .assertContainsColor(Color.Yellow)
    }

    @Test
    fun captureDialog_withOffset_verifyBackground_multiWindow() {
        setContent {
            Box(
                Modifier
                    .testTag(rootTag)
                    .size(300.dp)
                    .background(Color.Yellow)
            ) {
                Dialog({}) {
                    Box(Modifier.size(300.dp).background(Color.Red)) {
                        Text("Hello")
                    }
                }
            }
        }

        rule.onNodeWithTag(rootTag)
            .captureToImage(useAllWindows = true)
            .assertContainsColor(Color.Red)
    }

    @Test
    fun captureDialog_noShadow_verifyBackground_multiWindow() {
        setContent {
            Box(
                Modifier
                    .testTag(rootTag)
                    .size(300.dp)
                    .background(Color.Yellow)
            ) {
                Dialog({}) {
                    Box(Modifier.testTag("dialog").size(300.dp).background(Color.Red)) {
                        Text("Hello")
                    }
                }
            }
        }

        // Remove scrim
        val dialogWindowProvider =
            findDialogWindowProviderInParent(fetchNodeRootView("dialog"))
        rule.runOnUiThread {
            val originalAttributes = dialogWindowProvider?.window?.attributes
            originalAttributes?.dimAmount = 0.0f

            // Reapply flag
            dialogWindowProvider?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindowProvider?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        rule.onNodeWithTag(rootTag)
            .captureToImage(useAllWindows = true)
            .assertContainsColor(Color.Red)
            .assertContainsColor(Color.Yellow)
    }

    @Test
    fun capturePopup_verifySize_multiWindow() {
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
            .captureToImage(useAllWindows = true)
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
                    Box(Modifier.padding(top = 20.toDp()).background(colorBg)) {
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
