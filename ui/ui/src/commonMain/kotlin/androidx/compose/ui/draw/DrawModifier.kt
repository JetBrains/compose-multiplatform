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

package androidx.compose.ui.draw

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
interface DrawModifier : Modifier.Element {

    fun ContentDrawScope.draw()
}

/**
 * [DrawModifier] implementation that supports building a cache of objects
 * to be referenced across draw calls
 */
interface DrawCacheModifier : DrawModifier {

    /**
     * Callback invoked to re-build objects to be re-used across draw calls.
     * This is useful to conditionally recreate objects only if the size of the
     * drawing environment changes, or if state parameters that are inputs
     * to objects change. This method is guaranteed to be called before
     * [DrawModifier.draw].
     *
     * @param size The current size of the drawing environment
     * @param density The current screen density to provide the ability to convert between
     * density independent and raw pixel values
     */
    fun onBuildCache(size: Size, density: Density)
}

/**
 * Draw into a [Canvas] behind the modified content.
 */
fun Modifier.drawBehind(
    onDraw: DrawScope.() -> Unit
) = this.then(
    DrawBackgroundModifier(
        onDraw = onDraw,
        inspectorInfo = debugInspectorInfo {
            name = "drawBehind"
            properties["onDraw"] = onDraw
        }
    )
)

private class DrawBackgroundModifier(
    val onDraw: DrawScope.() -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawModifier, InspectorValueInfo(inspectorInfo) {
    override fun ContentDrawScope.draw() {
        onDraw()
        drawContent()
    }
}

/**
 * Draw into a [DrawScope] with content that is persisted across
 * draw calls as long as the size of the drawing area is the same or
 * any state objects that are read have not changed. In the event that
 * the drawing area changes, or the underlying state values that are being read
 * change, this method is invoked again to recreate objects to be used during drawing
 *
 * For example, a [androidx.compose.ui.graphics.LinearGradient] that is to occupy the full
 * bounds of the drawing area can be created once the size has been defined and referenced
 * for subsequent draw calls without having to re-allocate.
 *
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierSample
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierStateParameterSample
 * @sample androidx.compose.ui.samples.DrawWithCacheContentSample
 */
fun Modifier.drawWithCache(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "drawWithCache"
        properties["onBuildDrawCache"] = onBuildDrawCache
    }
) {
    val cacheDrawScope = remember { CacheDrawScope() }
    this.then(DrawContentCacheModifier(cacheDrawScope, onBuildDrawCache))
}

/**
 * Handle to a drawing environment that enables caching of content based on the resolved size.
 * Consumers define parameters and refer to them in the captured draw callback provided in
 * [onDrawBehind] or [onDrawWithContent].
 *
 * [onDrawBehind] will draw behind the layout's drawing contents however, [onDrawWithContent] will
 * provide the ability to draw before or after the layout's contents
 */
class CacheDrawScope internal constructor(
    internal var cachedDrawDensity: Density? = null
) : Density {
    internal var drawResult: DrawResult? = null

    /**
     * Provides the dimensions of the current drawing environment
     */
    var size: Size = Size.Unspecified
        internal set

    /**
     * Issue drawing commands to be executed before the layout content is drawn
     */
    fun onDrawBehind(block: DrawScope.() -> Unit): DrawResult = onDrawWithContent {
        block()
        drawContent()
    }

    /**
     * Issue drawing commands before or after the layout's drawing contents
     */
    fun onDrawWithContent(block: ContentDrawScope.() -> Unit): DrawResult {
        return DrawResult(block).also { drawResult = it }
    }

    override val density: Float
        get() = cachedDrawDensity!!.density

    override val fontScale: Float
        get() = cachedDrawDensity!!.density
}

/**
 * DrawCacheModifier implementation that is used to construct objects that are dependent on
 * the drawing area and re-used across draw calls
 */
private data class DrawContentCacheModifier(
    val cacheDrawScope: CacheDrawScope,
    val onBuildDrawCache: CacheDrawScope.() -> DrawResult
) : DrawCacheModifier {

    override fun onBuildCache(size: Size, density: Density) {
        cacheDrawScope.apply {
            cachedDrawDensity = density
            this.size = size
            drawResult = null
            onBuildDrawCache()
            checkNotNull(drawResult) {
                "DrawResult not defined, did you forget to call onDraw?"
            }
        }
    }

    override fun ContentDrawScope.draw() {
        cacheDrawScope.drawResult!!.block(this)
    }
}

/**
 * Holder to a callback to be invoked during draw operations. This lambda
 * captures and reuses parameters defined within the CacheDrawScope receiver scope lambda.
 */
class DrawResult internal constructor(internal var block: ContentDrawScope.() -> Unit)

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's
 * contents. It also allows the modifier to adjust the layout's canvas.
 */
// TODO: Inline this function -- it breaks with current compiler
/*inline*/ fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier = this.then(
    object : DrawModifier, InspectorValueInfo(
        debugInspectorInfo {
            name = "drawWithContent"
            properties["onDraw"] = onDraw
        }
    ) {
        override fun ContentDrawScope.draw() {
            onDraw()
        }
    }
)
