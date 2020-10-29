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

import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import org.jetbrains.skija.Surface
import org.junit.After
import org.junit.Rule

abstract class DesktopGraphicsTest {
    @get:Rule
    val screenshotRule = DesktopScreenshotTestRule("ui/ui-desktop/graphics")

    private var _surface: Surface? = null
    protected val surface get() = _surface!!

    protected val redPaint = Paint().apply { color = Color.Red }
    protected val bluePaint = Paint().apply { color = Color.Blue }
    protected val greenPaint = Paint().apply { color = Color.Green }
    protected val cyanPaint = Paint().apply { color = Color.Cyan }
    protected val magentaPaint = Paint().apply { color = Color.Magenta }
    protected val yellowPaint = Paint().apply { color = Color.Yellow }

    @Suppress("SameParameterValue")
    protected fun initCanvas(widthPx: Int, heightPx: Int): Canvas {
        require(_surface == null)
        _surface = Surface.makeRasterN32Premul(widthPx, heightPx)
        return DesktopCanvas(_surface!!.canvas)
    }

    @After
    fun teardown() {
        _surface?.close()
    }
}