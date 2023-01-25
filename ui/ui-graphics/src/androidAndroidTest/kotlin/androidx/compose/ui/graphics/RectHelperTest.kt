/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class RectHelperTest {

    @Suppress("DEPRECATION")
    @Test
    fun rectToAndroidRectTruncates() {
        assertEquals(
            android.graphics.Rect(
                2,
                3,
                4,
                5
            ),
            Rect(
                2f,
                3.1f,
                4.5f,
                5.99f
            ).toAndroidRect()
        )
    }

    @Test
    fun rectToAndroidRectFConverts() {
        assertEquals(
            android.graphics.RectF(
                2f,
                3.1f,
                4.5f,
                5.99f
            ),
            Rect(
                2f,
                3.1f,
                4.5f,
                5.99f
            ).toAndroidRectF()
        )
    }

    @Test
    fun androidRectToRectConverts() {
        assertEquals(
            Rect(
                2f,
                3f,
                4f,
                5f
            ),
            android.graphics.Rect(
                2,
                3,
                4,
                5
            ).toComposeRect(),
        )
    }

    @Test
    fun intRectToAndroidRectConverts() {
        assertEquals(
            android.graphics.Rect(
                2,
                3,
                4,
                5
            ),
            IntRect(
                2,
                3,
                4,
                5
            ).toAndroidRect(),
        )
    }

    @Test
    fun androidRectToIntRectConverts() {
        assertEquals(
            IntRect(
                2,
                3,
                4,
                5
            ),
            android.graphics.Rect(
                2,
                3,
                4,
                5
            ).toComposeIntRect(),
        )
    }
}
