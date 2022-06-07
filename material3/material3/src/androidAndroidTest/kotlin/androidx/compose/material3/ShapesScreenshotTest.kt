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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class ShapesScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun shapes() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.semantics(mergeDescendants = true) {}.testTag(Tag)) {
                val shapes = MaterialTheme.shapes
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Button(shape = Shapes.None, onClick = {}) { Text("None") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(shape = shapes.extraSmall, onClick = {}) { Text("Extra  Small") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(shape = shapes.small, onClick = {}) { Text("Small") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(shape = shapes.medium, onClick = {}) { Text("Medium") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(shape = shapes.large, onClick = {}) { Text("Large") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(shape = shapes.extraLarge, onClick = {}) { Text("Extra Large") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(shape = Shapes.Full, onClick = {}) { Text("Full") }
                }
            }
        }
        assertAgainstGolden("shapes")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    private val Tag = "Shapes"
}
