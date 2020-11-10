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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class ImageBitmapTest {

    @Test
    fun testCreatedImage() {
        val cs = ColorSpaces.Srgb
        val image = ImageBitmap(
            width = 10,
            height = 20,
            config = ImageBitmapConfig.Argb8888,
            hasAlpha = false,
            colorSpace = cs
        )

        assertEquals(10, image.width)
        assertEquals(20, image.height)
        assertEquals(ImageBitmapConfig.Argb8888, image.config)
        assertFalse(image.hasAlpha)
        assertEquals(cs, image.colorSpace)
    }
}