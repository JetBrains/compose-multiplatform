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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class)
class BadgeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun lightTheme_noContent() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(
                Modifier.size(56.dp).semantics(mergeDescendants = true) {}.testTag(TestTag),
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(badge = { Badge() }) {
                    Icon(Icons.Filled.Favorite, null)
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_noContent"
        )
    }

    @Test
    fun darkTheme_noContent() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(
                Modifier.size(56.dp).semantics(mergeDescendants = true) {}.testTag(TestTag),
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(badge = { Badge() }) {
                    Icon(Icons.Filled.Favorite, null)
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_darkTheme_noContent"
        )
    }

    @Test
    fun lightTheme_withContent() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(
                Modifier.size(56.dp).semantics(mergeDescendants = true) {}.testTag(TestTag),
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(badge = { Badge { Text("8") } }) {
                    Icon(Icons.Filled.Favorite, null)
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_lightTheme_withContent"
        )
    }

    @Test
    fun darkTheme_withContent() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(
                Modifier.size(56.dp).semantics(mergeDescendants = true) {}.testTag(TestTag),
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(badge = { Badge { Text("8") } }) {
                    Icon(Icons.Filled.Favorite, null)
                }
            }
        }

        assertBadgeAgainstGolden(
            goldenIdentifier = "badge_darkTheme_withContent"
        )
    }

    private fun assertBadgeAgainstGolden(goldenIdentifier: String) {
        composeTestRule.onNodeWithTag(TestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

private const val TestTag = "badge"
