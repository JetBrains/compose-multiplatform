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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun ImageSample() {
    val ImageBitmap = createTestImage()
    // Lays out and draws an image sized to the dimensions of the ImageBitmap
    Image(bitmap = ImageBitmap, contentDescription = "Localized description")
}

@Sampled
@Composable
fun BitmapPainterSubsectionSample() {
    val ImageBitmap = createTestImage()
    // Lays out and draws an image sized to the rectangular subsection of the ImageBitmap
    Image(
        painter = BitmapPainter(
            ImageBitmap,
            IntOffset(10, 12),
            IntSize(50, 60)
        ),
        contentDescription = "Localized description"
    )
}

@Sampled
@Composable
fun BitmapPainterSample() {
    val customPainter = remember {
        object : Painter() {

            override val intrinsicSize: Size
                get() = Size(100.0f, 100.0f)

            override fun DrawScope.onDraw() {
                drawRect(color = Color.Cyan)
            }
        }
    }

    Image(
        painter = customPainter,
        contentDescription = "Localized description",
        modifier = Modifier.size(100.dp, 100.dp)
    )
}

/**
 * Helper method to create an ImageBitmap with some content in it
 */
private fun createTestImage(): ImageBitmap {
    val ImageBitmap = ImageBitmap(100, 100)
    Canvas(ImageBitmap).drawCircle(
        Offset(50.0f, 50.0f), 50.0f,
        Paint().apply { this.color = Color.Cyan }
    )
    return ImageBitmap
}