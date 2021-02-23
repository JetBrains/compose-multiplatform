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

package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.DrawCache
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skija.Data
import org.jetbrains.skija.Point
import org.jetbrains.skija.svg.DOM
import java.io.InputStream
import kotlin.math.ceil

/**
 * Synchronously load an SVG image stored in resources for the application.
 *
 * @param resourcePath path to the file in the resources folder
 * @return the decoded vector image associated with the resource
 */
@Composable
fun svgResource(resourcePath: String): Painter {
    val density = LocalDensity.current
    return remember(resourcePath, density) {
        openResourceStream(resourcePath).use {
            loadSvgResource(it, density)
        }
    }
}

/**
 * Synchronously load an SVG image from some [inputStream].
 *
 * In contrast to [svgResource] this function isn't [Composable]
 *
 * @param inputStream input stream to load an SVG resource. All bytes will be read from this stream,
 *        but stream will not be closed after this method.
 * @return the decoded SVG image associated with the resource
 */
fun loadSvgResource(inputStream: InputStream, density: Density): Painter {
    val data = Data.makeFromBytes(inputStream.readAllBytes())
    return SVGPainter(DOM(data), density)
}

private class SVGPainter(
    private val dom: DOM,
    private val density: Density
) : Painter() {
    private val defaultSizePx: Size = run {
        val containerSize = dom.containerSize
        if (containerSize.x == 0f && containerSize.y == 0f) {
            Size.Unspecified
        } else {
            Size(containerSize.x, containerSize.y)
        }
    }

    override val intrinsicSize: Size get() {
        return if (defaultSizePx.isSpecified) {
            defaultSizePx * density.density
        } else {
            Size.Unspecified
        }
    }

    private var previousDrawSize: Size = Size.Unspecified
    private var alpha: Float = 1.0f
    private var colorFilter: ColorFilter? = null

    // with caching into bitmap FPS is 3x-4x higher (tested with idea-logo.svg with 30x30 icons)
    private val drawCache = DrawCache()

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        if (previousDrawSize != size) {
            drawCache.drawCachedImage(
                IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
                density = this,
                layoutDirection,
            ) {
                drawSvg(size)
            }
        }

        drawCache.drawInto(this, alpha, colorFilter)
    }

    private fun DrawScope.drawSvg(size: Size) {
        drawIntoCanvas {
            dom.containerSize = Point(size.width, size.height)
            dom.render(it.nativeCanvas)
        }
    }
}