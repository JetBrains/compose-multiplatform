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
import androidx.compose.runtime.Providers
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Box
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredSize
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class CardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(8.dp)
        val background = Color.Yellow
        var cardColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Surface(color = background) {
                Stack {
                    cardColor = MaterialTheme.colors.surface
                    Providers(ShapesAmbient provides Shapes(medium = shape)) {
                        Card(modifier = Modifier
                            .semantics(mergeAllDescendants = true) {}
                            .testTag("card"),
                            elevation = 0.dp
                        ) {
                            Box(Modifier.preferredSize(50.dp, 50.dp))
                        }
                    }
                }
            }
        }

        onNodeWithTag("card")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = shape,
                shapeColor = cardColor,
                backgroundColor = background,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }
}
