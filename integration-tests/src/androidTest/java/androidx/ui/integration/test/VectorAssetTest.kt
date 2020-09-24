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

package androidx.ui.integration.test

import android.os.Build
import androidx.compose.foundation.Image
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.ui.test.captureToBitmap
import androidx.ui.integration.test.framework.ProgrammaticVectorTestCase
import androidx.ui.integration.test.framework.XmlVectorTestCase
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.roundToInt

/**
 * Test to ensure that [XmlVectorTestCase] and [ProgrammaticVectorTestCase] have an identical pixel
 * output when ran. This ensures correctness for the corresponding benchmarks.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class VectorAssetTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testProgrammaticAndXmlVectorAssetsAreTheSame() {
        val xmlTestCase = XmlVectorTestCase()
        val programmaticTestCase = ProgrammaticVectorTestCase()

        rule.setContent {
            Column {
                xmlTestCase.emitContent()
                programmaticTestCase.emitContent()
            }
        }

        val xmlBitmap = rule.onNodeWithTag(xmlTestCase.testTag).captureToBitmap()
        val programmaticBitmap = rule.onNodeWithTag(programmaticTestCase.testTag).captureToBitmap()

        assertEquals(xmlBitmap.width, programmaticBitmap.width)
        assertEquals(xmlBitmap.height, programmaticBitmap.height)

        val xmlPixelArray = with(xmlBitmap) {
            IntArray(width * height).apply {
                getPixels(this, 0, width, 0, 0, width, height)
            }
        }

        val programmaticBitmapArray = with(programmaticBitmap) {
            IntArray(width * height).apply {
                getPixels(this, 0, width, 0, 0, width, height)
            }
        }

        assertArrayEquals(xmlPixelArray, programmaticBitmapArray)
    }

    @Test
    fun testEvenOddPathType() {
        val testTag = "testTag"
        var insetRectSize: Int = 0
        rule.setContent {
            with(DensityAmbient.current) {
                insetRectSize = (10f * this.density).roundToInt()
            }
            val vectorAsset =
                vectorResource(R.drawable.ic_pathfill_sample)
            Image(vectorAsset, modifier = Modifier.testTag(testTag))
        }

        rule.onNodeWithTag(testTag).captureToBitmap().apply {
            assertEquals(Color.Blue.toArgb(), getPixel(0, 0))
            assertEquals(Color.Blue.toArgb(), getPixel(width - 1, 0))
            assertEquals(Color.Blue.toArgb(), getPixel(0, height - 1))
            assertEquals(Color.Blue.toArgb(), getPixel(width - 1, height - 1))

            assertEquals(Color.Blue.toArgb(), getPixel(width / 2, height / 2))

            assertEquals(Color.Red.toArgb(), getPixel(insetRectSize + 2, insetRectSize + 2))
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    width - insetRectSize - 2,
                    insetRectSize + 2
                )
            )
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    insetRectSize + 2,
                    height - insetRectSize - 2
                )
            )
            assertEquals(
                Color.Red.toArgb(),
                getPixel(
                    width - insetRectSize - 2,
                    height -
                        insetRectSize - 2
                )
            )
        }
    }
}
