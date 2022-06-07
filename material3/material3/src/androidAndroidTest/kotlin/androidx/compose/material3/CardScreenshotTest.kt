/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
class CardScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.size(width = 200.dp, height = 120.dp)
    private val wrapperTestTag = "cardWrapper"

    @Test
    fun filledCard_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(Modifier.size(width = 180.dp, height = 100.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("filledCard_lightTheme")
    }

    @Test
    fun filledCard_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(Modifier.size(width = 180.dp, height = 100.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("filledCard_darkTheme")
    }

    @Test
    fun filledCard_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp),
                    enabled = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("filledCard_disabled_lightTheme")
    }

    @Test
    fun filledCard_disabled_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp),
                    enabled = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("filledCard_disabled_darkTheme")
    }

    @Test
    fun elevatedCard_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(Modifier.size(width = 180.dp, height = 100.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("elevatedCard_lightTheme")
    }

    @Test
    fun elevatedCard_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(Modifier.size(width = 180.dp, height = 100.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("elevatedCard_darkTheme")
    }

    @Test
    fun elevatedCard_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp),
                    enabled = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("elevatedCard_disabled_lightTheme")
    }

    @Test
    fun elevatedCard_disabled_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp),
                    enabled = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("elevatedCard_disabled_darkTheme")
    }

    @Test
    fun outlinedCard_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(Modifier.size(width = 180.dp, height = 100.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedCard_lightTheme")
    }

    @Test
    fun outlinedCard_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(Modifier.size(width = 180.dp, height = 100.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedCard_darkTheme")
    }

    @Test
    fun outlinedCard_disabled_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp),
                    enabled = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedCard_disabled_lightTheme")
    }

    @Test
    fun outlinedCard_disabled_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp),
                    enabled = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedCard_disabled_darkTheme")
    }

    @Test
    fun filledCard_pressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        assertPressed("filledCard_pressed")
    }

    @Test
    fun elevatedCard_pressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        assertPressed("elevatedCard_pressed")
    }

    @Test
    fun outlinedCard_pressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        assertPressed("outlinedCard_pressed")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun filledCard_hover() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        rule.onNode(hasClickAction()).performMouseInput { enter(center) }
        rule.waitForIdle()

        assertAgainstGolden("filledCard_hover")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun elevatedCard_hover() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        rule.onNode(hasClickAction()).performMouseInput { enter(center) }
        rule.waitForIdle()

        assertAgainstGolden("elevatedCard_hover")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun outlinedCard_hover() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        rule.onNode(hasClickAction()).performMouseInput { enter(center) }
        rule.waitForIdle()

        assertAgainstGolden("outlinedCard_hover")
    }

    @Test
    fun filledCard_focused() {
        val focusRequester = FocusRequester()
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                Card(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Filled Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.waitForIdle()

        assertAgainstGolden("filledCard_focus")
    }

    @Test
    fun elevatedCard_focused() {
        val focusRequester = FocusRequester()
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                ElevatedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Elevated Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.waitForIdle()

        assertAgainstGolden("elevatedCard_focused")
    }

    @Test
    fun outlinedCard_focused() {
        val focusRequester = FocusRequester()
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag), contentAlignment = Alignment.Center) {
                OutlinedCard(
                    onClick = {},
                    Modifier.size(width = 180.dp, height = 100.dp)
                        // Normally this is only focusable in non-touch mode, so let's force it to
                        // always be focusable so we can test how it appears
                        .focusProperties { canFocus = true }
                        .focusRequester(focusRequester)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Outlined Card",
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        rule.runOnIdle { focusRequester.requestFocus() }
        rule.waitForIdle()

        assertAgainstGolden("outlinedCard_focused")
    }

    private fun assertPressed(goldenName: String) {
        rule.mainClock.autoAdvance = false
        rule.onNode(hasClickAction()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden(goldenName)
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
