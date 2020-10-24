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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.PI

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathTest {

    @Test
    fun testAddArcPath() {
        val width = 100
        val height = 100
        val image = ImageAsset(width, height)
        val canvas = Canvas(image)
        val path1 = Path().apply {
            addArcRad(
                Rect(Offset.Zero, Size(width.toFloat(), height.toFloat())),
                0.0f,
                PI.toFloat() / 2
            )
        }

        val arcColor = Color.Cyan
        val arcPaint = Paint().apply { color = arcColor }
        canvas.drawPath(path1, arcPaint)

        val path2 = Path().apply {
            arcToRad(
                Rect(Offset(0.0f, 0.0f), Size(width.toFloat(), height.toFloat())),
                PI.toFloat(),
                PI.toFloat() / 2,
                false
            )
            close()
        }

        canvas.drawPath(path2, arcPaint)

        val pixelmap = image.toPixelMap()
        val x = (50.0 * Math.cos(PI / 4)).toInt()
        assertEquals(
            arcColor,
            pixelmap[
                width / 2 + x - 1,
                height / 2 + x - 1
            ]
        )

        assertEquals(
            arcColor,
            pixelmap[
                width / 2 - x,
                height / 2 - x
            ]
        )
    }
}