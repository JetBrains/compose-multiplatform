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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
@OptIn(ExperimentalMaterialApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class DrawerScreenshotTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    private fun ComposeContentTestRule.setBottomDrawer(drawerValue: BottomDrawerValue) {
        setMaterialContent {
            Box(Modifier.requiredSize(10.dp, 100.dp).testTag("container")) {
                BottomDrawer(
                    drawerState = rememberBottomDrawerState(drawerValue),
                    drawerContent = { Box(Modifier.fillMaxSize().background(Color.Red)) },
                    content = { Box(Modifier.fillMaxSize().background(Color.Yellow)) }
                )
            }
        }
    }

    private fun ComposeContentTestRule.setModalDrawer(drawerValue: DrawerValue) {
        setMaterialContent {
            Box(Modifier.requiredSize(100.dp, 10.dp).testTag("container")) {
                ModalDrawer(
                    drawerState = rememberDrawerState(drawerValue),
                    drawerContent = { Box(Modifier.fillMaxSize().background(Color.Red)) },
                    content = { Box(Modifier.fillMaxSize().background(Color.Yellow)) }
                )
            }
        }
    }

    @Test
    fun bottomDrawer_closed() {
        rule.setBottomDrawer(BottomDrawerValue.Closed)
        assertScreenshotAgainstGolden("bottomDrawer_closed")
    }

    @Test
    fun modalDrawer_closed() {
        rule.setModalDrawer(DrawerValue.Closed)
        assertScreenshotAgainstGolden("modalDrawer_closed")
    }

    @Test
    fun bottomDrawer_open() {
        rule.setBottomDrawer(BottomDrawerValue.Open)
        assertScreenshotAgainstGolden("bottomDrawer_opened")
    }

    @Test
    fun modalDrawer_open() {
        rule.setModalDrawer(DrawerValue.Open)
        assertScreenshotAgainstGolden("modalDrawer_opened")
    }

    private fun assertScreenshotAgainstGolden(goldenName: String) {
        rule.onNodeWithTag("container")
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
