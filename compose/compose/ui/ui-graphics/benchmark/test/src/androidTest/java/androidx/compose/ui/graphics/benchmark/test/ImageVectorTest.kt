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

package androidx.compose.ui.graphics.benchmark.test

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.benchmark.ProgrammaticVectorTestCase
import androidx.compose.ui.graphics.benchmark.XmlVectorTestCase
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * Test to ensure that [XmlVectorTestCase] and [ProgrammaticVectorTestCase] have an identical pixel
 * output when ran. This ensures correctness for the corresponding benchmarks.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class ImageVectorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testProgrammaticAndXmlImageVectorsAreTheSame() {
        val xmlTestCase = XmlVectorTestCase()
        val programmaticTestCase = ProgrammaticVectorTestCase()

        rule.setContent {
            Column {
                xmlTestCase.Content()
                programmaticTestCase.Content()
            }
        }

        val xmlBitmap = rule.onNodeWithTag(xmlTestCase.testTag).captureToImage().asAndroidBitmap()
        val programmaticBitmap = rule.onNodeWithTag(programmaticTestCase.testTag).captureToImage()
            .asAndroidBitmap()

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

        Assert.assertArrayEquals(xmlPixelArray, programmaticBitmapArray)
    }

    @Test
    fun testAutoMirror() {
        val testTag = "AutoMirrorImage"
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box {
                    Image(
                        modifier = Modifier.testTag(testTag),
                        painter = painterResource(
                            androidx.compose.ui.graphics.benchmark.R.drawable.ic_auto_mirror
                        ),
                        contentDescription = null
                    )
                }
            }
        }
        rule.onNodeWithTag(testTag).captureToImage().toPixelMap().apply {
            assertEquals(Color.Blue, this[2, 2])
            assertEquals(Color.Blue, this[2, height - 3])
            assertEquals(Color.Blue, this[width / 2 - 3, 2])
            assertEquals(Color.Blue, this[width / 2 - 3, height - 3])

            assertEquals(Color.Red, this[width - 3, 2])
            assertEquals(Color.Red, this[width - 3, height - 3])
            assertEquals(Color.Red, this[width / 2 + 3, 2])
            assertEquals(Color.Red, this[width / 2 + 3, height - 3])
        }
    }

    @Test
    fun testEvenOddPathType() {
        val testTag = "testTag"
        var insetRectSize: Int = 0
        rule.setContent {
            with(LocalDensity.current) {
                insetRectSize = (10f * this.density).roundToInt()
            }
            val imageVector = painterResource(
                androidx.compose.ui.graphics.benchmark.R.drawable.ic_pathfill_sample
            )
            Image(imageVector, null, modifier = Modifier.testTag(testTag))
        }

        rule.onNodeWithTag(testTag).captureToImage().asAndroidBitmap().apply {
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