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

package androidx.compose.material

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterialApi::class)
@LargeTest
@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class BottomSheetScaffoldScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL)

    private val scaffoldTestTag = "scaffold_tag"

    @Test
    fun bottomSheetScaffold_topBar_drawnOver_body() {
        composeTestRule.setMaterialContent {
            BottomSheetScaffold(
                modifier = Modifier.testTag(scaffoldTestTag),
                topBar = {
                    // We deliberately use white to make sure the shadow is noticeable
                    TopAppBar(backgroundColor = Color.White) { Text("BottomSheetScaffold") }
                },
                sheetContent = {
                    Box(Modifier.height(100.dp))
                },
                content = {
                    Box(
                        Modifier
                            .background(Color.White)
                            .fillMaxSize())
                }
            )
        }

        composeTestRule.onNodeWithTag(scaffoldTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "bottomSheetScaffold_topBar_with_elevation"
            )
    }
}
