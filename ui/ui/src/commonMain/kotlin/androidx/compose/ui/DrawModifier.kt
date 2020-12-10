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

package androidx.compose.ui

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Shadow copy of the DrawModifier.kt file that now lives in the androidx.compose.ui.draw package
 * This keeps the original method signature with the original package name to assist in migration
 * from deprecated APIs to the current APIs.
 *
 * All new API changes should be made to androidx.compose.ui.draw.DrawModifier and not this file
 */

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
@Deprecated(
    "Use DrawModifier from the androidx.compose.ui.draw package instead",
    ReplaceWith(
        "DrawModifier",
        "androidx.compose.ui.draw.DrawModifier"
    )
)
typealias DrawModifier = androidx.compose.ui.draw.DrawModifier

/**
 * [DrawModifier] implementation that supports building a cache of objects
 * to be referenced across draw calls
 */
@Deprecated(
    "Use DrawCacheModifier from the androidx.compose.ui.draw package instead",
    ReplaceWith(
        "DrawCacheModifier",
        "androidx.compose.ui.draw.DrawCacheModifier"
    )
)
typealias DrawCacheModifier = androidx.compose.ui.draw.DrawCacheModifier

/**
 * Draw into a [Canvas] behind the modified content.
 */
@Deprecated(
    "Use drawBehind from the androidx.compose.ui.draw package instead",
    ReplaceWith(
        "drawBehind(onDraw)",
        "androidx.compose.ui.draw.drawBehind"
    )
)
fun Modifier.drawBehind(onDraw: DrawScope.() -> Unit) = drawBehind(onDraw)

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
@Suppress("DEPRECATION")
@Deprecated(
    "Use the drawWithCache from the androidx.compose.ui.draw package instead",
    ReplaceWith(
        "drawWithCache(onBuildCache)",
        "androidx.compose.ui.draw.drawWithCache"
    )
)
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> DrawResult) =
    drawWithCache(onBuildDrawCache)

/**
 * Handle to a drawing environment that enables caching of content based on the resolved size.
 * Consumers define parameters and refer to them in the captured draw callback provided in
 * [onDrawBehind] or [onDrawWithContent].
 *
 * [onDrawBehind] will draw behind the layout's drawing contents however, [onDrawWithContent] will
 * provide the ability to draw before or after the layout's contents
 */
@Deprecated(
    "Use CacheDrawScope from the androidx.compose.ui.draw package instead",
    ReplaceWith("CacheDrawScope", "androidx.compose.ui.draw.CacheDrawScope")
)
typealias CacheDrawScope = androidx.compose.ui.draw.CacheDrawScope

/**
 * Holder to a callback to be invoked during draw operations. This lambda
 * captures and reuses parameters defined within the CacheDrawScope receiver scope lambda.
 */
@Deprecated(
    "Use DrawResult from the androidx.compose.ui.draw package instead",
    ReplaceWith("DrawResult", "androidx.compose.ui.draw.DrawResult")
)
typealias DrawResult = androidx.compose.ui.draw.DrawResult

@Suppress("DEPRECATION")
@Deprecated(
    "Use drawWithContent from the androidx.compose.ui.draw package instead",
    ReplaceWith(
        "drawWithContent(onDraw)",
        "androidx.compose.ui.draw.drawWithContent"
    )
)
fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit) = drawWithContent(onDraw)
