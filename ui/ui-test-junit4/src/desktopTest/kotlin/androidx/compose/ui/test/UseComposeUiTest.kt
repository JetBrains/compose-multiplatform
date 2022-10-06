/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.nio.file.Files
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class UseComposeUiTest {
    @Test
    fun testDrawSquare() = runDesktopComposeUiTest(10, 10) {
        setContent {
            Canvas(Modifier.size(10.dp)) {
                drawRect(Color.Blue, size = Size(10f, 10f))
            }
        }
        val img: Image = captureToImage()
        val actualPng = Files.createTempFile("test-draw-square", ".png")
        val actualImage =
            img.encodeToData(EncodedImageFormat.PNG) ?: error("Could not encode image as png")
        actualPng.writeBytes(actualImage.bytes)

        val expectedPng =
            ClassLoader.getSystemResource("androidx/compose/ui/test/draw-square.png")

        assert(actualPng.readBytes().contentEquals(expectedPng.readBytes())) {
            "The actual image '$actualPng' does not match the expected image '$expectedPng'"
        }
    }
}
