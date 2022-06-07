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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
@OptIn(ExperimentalMaterial3Api::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ModalNavigationDrawerScreenshotTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private fun ComposeContentTestRule.setnavigationDrawer(drawerValue: DrawerValue) {
        setMaterialContent(lightColorScheme()) {
            Box(Modifier.requiredSize(400.dp, 32.dp).testTag(ContainerTestTag)) {
                ModalNavigationDrawer(
                    drawerState = rememberDrawerState(drawerValue),
                    drawerContent = {},
                    content = {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        )
                    }
                )
            }
        }
    }

    private fun ComposeContentTestRule.setDarknavigationDrawer(drawerValue: DrawerValue) {
        setMaterialContent(darkColorScheme()) {
            Surface {
                Box(Modifier.requiredSize(400.dp, 32.dp).testTag(ContainerTestTag)) {
                    ModalNavigationDrawer(
                        drawerState = rememberDrawerState(drawerValue),
                        drawerContent = {},
                        content = {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        }
                    )
                }
            }
        }
    }

    @Test
    fun lightTheme_navigationDrawer_closed() {
        rule.setnavigationDrawer(DrawerValue.Closed)
        assertScreenshotAgainstGolden("navigationDrawer_closed")
    }

    @Test
    fun lightTheme_navigationDrawer_open() {
        rule.setnavigationDrawer(DrawerValue.Open)
        assertScreenshotAgainstGolden("navigationDrawer_light_opened")
    }

    @Test
    fun darkTheme_navigationDrawer_open() {
        rule.setDarknavigationDrawer(DrawerValue.Open)
        assertScreenshotAgainstGolden("navigationDrawer_dark_opened")
    }

    private fun assertScreenshotAgainstGolden(goldenName: String) {
        rule.onNodeWithTag("container")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}

private val ContainerTestTag = "container"