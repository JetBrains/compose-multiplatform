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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.internal.JvmDefaultWithCompatibility

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
@JvmDefaultWithCompatibility
interface DrawModifier : Modifier.Element {

    fun ContentDrawScope.draw()
}

/**
 * [DrawModifier] implementation that supports building a cache of objects
 * to be referenced across draw calls
 */
@JvmDefaultWithCompatibility
interface DrawCacheModifier : DrawModifier {

    /**
     * Callback invoked to re-build objects to be re-used across draw calls.
     * This is useful to conditionally recreate objects only if the size of the
     * drawing environment changes, or if state parameters that are inputs
     * to objects change. This method is guaranteed to be called before
     * [DrawModifier.draw].
     *
     * @param params The params to be used to build the cache.
     */
    fun onBuildCache(params: BuildDrawCacheParams)
}

/**
 * The set of parameters which could be used to build the drawing cache.
 *
 * @see DrawCacheModifier.onBuildCache
 */
interface BuildDrawCacheParams {
    /**
     * The current size of the drawing environment
     */
    val size: Size

    /**
     * The current layout direction.
     */
    val layoutDirection: LayoutDirection

    /**
     * The current screen density to provide the ability to convert between
     */
    val density: Density
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBackgroundModifier) return false

        return onDraw == other.onDraw
    }

    override fun hashCode(): Int {
        return onDraw.hashCode()
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
class CacheDrawScope internal constructor() : Density {
    internal var cacheParams: BuildDrawCacheParams = EmptyBuildDrawCacheParams
    internal var drawResult: DrawResult? = null

    /**
     * Provides the dimensions of the current drawing environment
     */
    val size: Size get() = cacheParams.size

    /**
     * Provides the [LayoutDirection].
     */
    val layoutDirection: LayoutDirection get() = cacheParams.layoutDirection

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
        get() = cacheParams.density.density

    override val fontScale: Float
        get() = cacheParams.density.fontScale
}

private object EmptyBuildDrawCacheParams : BuildDrawCacheParams {
    override val size: Size = Size.Unspecified
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val density: Density = Density(1f, 1f)
}

/**
 * DrawCacheModifier implementation that is used to construct objects that are dependent on
 * the drawing area and re-used across draw calls
 */
private data class DrawContentCacheModifier(
    val cacheDrawScope: CacheDrawScope,
    val onBuildDrawCache: CacheDrawScope.() -> DrawResult
) : DrawCacheModifier {

    override fun onBuildCache(params: BuildDrawCacheParams) {
        cacheDrawScope.apply {
            cacheParams = params
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawContentCacheModifier) return false

        if (cacheDrawScope != other.cacheDrawScope) return false
        if (onBuildDrawCache != other.onBuildDrawCache) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cacheDrawScope.hashCode()
        result = 31 * result + onBuildDrawCache.hashCode()
        return result
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
fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier = this.then(
    DrawWithContentModifier(
        onDraw = onDraw,
        inspectorInfo = debugInspectorInfo {
            name = "drawWithContent"
            properties["onDraw"] = onDraw
        }
    )
)

private class DrawWithContentModifier(
    val onDraw: ContentDrawScope.() -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawModifier, InspectorValueInfo(inspectorInfo) {

    override fun ContentDrawScope.draw() {
        onDraw()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawWithContentModifier) return false

        return onDraw == other.onDraw
    }

    override fun hashCode(): Int {
        return onDraw.hashCode()
    }
}
