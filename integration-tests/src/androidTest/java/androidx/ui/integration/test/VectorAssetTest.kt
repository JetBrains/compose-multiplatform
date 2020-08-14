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
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.compose.foundation.layout.Column
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
import org.junit.runners.JUnit4

/**
 * Test to ensure that [XmlVectorTestCase] and [ProgrammaticVectorTestCase] have an identical pixel
 * output when ran. This ensures correctness for the corresponding benchmarks.
 */
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class VectorAssetTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun testProgrammaticAndXmlVectorAssetsAreTheSame() {
        val xmlTestCase = XmlVectorTestCase()
        val programmaticTestCase = ProgrammaticVectorTestCase()

        composeTestRule.setContent {
            Column {
                xmlTestCase.emitContent()
                programmaticTestCase.emitContent()
            }
        }

        val xmlBitmap = onNodeWithTag(xmlTestCase.testTag).captureToBitmap()
        val programmaticBitmap = onNodeWithTag(programmaticTestCase.testTag).captureToBitmap()

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
}
