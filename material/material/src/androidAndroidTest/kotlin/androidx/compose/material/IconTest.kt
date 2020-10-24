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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.ImagePainter
import androidx.compose.ui.graphics.vector.VectorAssetBuilder
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class IconTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun vector_materialIconSize_dimensions() {
        val width = 24.dp
        val height = 24.dp
        val vector = Icons.Filled.Menu
        rule
            .setMaterialContentForSizeAssertions {
                Icon(vector)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun vector_customIconSize_dimensions() {
        val width = 35.dp
        val height = 83.dp
        val vector = VectorAssetBuilder(
            defaultWidth = width, defaultHeight = height,
            viewportWidth = width.value, viewportHeight = height.value
        ).build()
        rule
            .setMaterialContentForSizeAssertions {
                Icon(vector)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun image_noIntrinsicSize_dimensions() {
        val width = 24.dp
        val height = 24.dp
        rule
            .setMaterialContentForSizeAssertions {
                val image = with(DensityAmbient.current) {
                    ImageAsset(width.toIntPx(), height.toIntPx())
                }

                Icon(image)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun image_withIntrinsicSize_dimensions() {
        val width = 35.dp
        val height = 83.dp

        rule
            .setMaterialContentForSizeAssertions {
                val image = with(DensityAmbient.current) {
                    ImageAsset(width.toIntPx(), height.toIntPx())
                }

                Icon(image)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun painter_noIntrinsicSize_dimensions() {
        val width = 24.dp
        val height = 24.dp
        val painter = ColorPainter(Color.Red)
        rule
            .setMaterialContentForSizeAssertions {
                Icon(painter)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }

    @Test
    fun painter_withIntrinsicSize_dimensions() {
        val width = 35.dp
        val height = 83.dp

        rule
            .setMaterialContentForSizeAssertions {
                val image = with(DensityAmbient.current) {
                    ImageAsset(width.toIntPx(), height.toIntPx())
                }

                val imagePainter = ImagePainter(image)
                Icon(imagePainter)
            }
            .assertWidthIsEqualTo(width)
            .assertHeightIsEqualTo(height)
    }
}
