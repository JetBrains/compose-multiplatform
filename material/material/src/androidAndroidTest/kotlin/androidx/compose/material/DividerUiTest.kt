/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.test.filters.MediumTest
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DividerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultHeight = 1.dp

    @Test
    fun divider_DefaultSizes() {
        composeTestRule
            .setMaterialContentForSizeAssertions {
                Divider()
            }
            .assertHeightIsEqualTo(defaultHeight)
            .assertWidthFillsRoot()
    }

    @Test
    fun divider_CustomSizes() {
        val height = 20.dp
        composeTestRule
            .setMaterialContentForSizeAssertions {
                Divider(thickness = height)
            }
            .assertWidthFillsRoot()
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun divider_SizesWithIndent_DoesNotChanged() {
        val indent = 75.dp
        val height = 21.dp

        composeTestRule
            .setMaterialContentForSizeAssertions {
                Divider(startIndent = indent, thickness = height)
            }
            .assertHeightIsEqualTo(height)
            .assertWidthFillsRoot()
    }
}