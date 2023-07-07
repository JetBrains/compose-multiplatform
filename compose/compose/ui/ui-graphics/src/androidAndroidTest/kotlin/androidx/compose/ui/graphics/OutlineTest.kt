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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OutlineTest {

    @Test
    fun testRectOutlineBounds() {
        val outlineRect = Outline.Rectangle(Rect(1f, 2f, 3f, 4f))
        assertEquals(Rect(1f, 2f, 3f, 4f), outlineRect.bounds)
    }

    @Test
    fun testRoundRectOutlineBounds() {
        val roundRectOutline = Outline.Rounded(
            RoundRect(5f, 10f, 15f, 20f, CornerRadius(7f))
        )
        assertEquals(Rect(5f, 10f, 15f, 20f), roundRectOutline.bounds)
    }

    @Test
    fun testPathOutlineBounds() {
        val pathOutline = Outline.Generic(
            Path().apply {
                moveTo(5f, 15f)
                lineTo(100f, 200f)
                lineTo(0f, 200f)
                close()
            }
        )
        assertEquals(Rect(0f, 15f, 100f, 200f), pathOutline.bounds)
    }
}