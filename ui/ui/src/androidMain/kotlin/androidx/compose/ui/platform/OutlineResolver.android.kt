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

package androidx.compose.ui.platform

import android.os.Build
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import android.graphics.Outline as AndroidOutline

/**
 * Resolves the [AndroidOutline] from the [Shape] of an [OwnedLayer].
 */
internal class OutlineResolver(private var density: Density) {

    /**
     * Flag to determine if the shape specified on the outline is supported.
     * On older API levels, concave shapes are not allowed
     */
    private var isSupportedOutline = true

    /**
     * The Android Outline that is used in the layer.
     */
    private val cachedOutline = AndroidOutline().apply { alpha = 1f }

    /**
     * The size of the layer. This is used in generating the [Outline] from the [Shape].
     */
    private var size: Size = Size.Zero

    /**
     * The [Shape] of the Outline of the Layer.
     */
    private var shape: Shape = RectangleShape

    /**
     * Asymmetric rounded rectangles need to use a Path. This caches that Path so that
     * a new one doesn't have to be generated each time.
     */
    // TODO(andreykulikov): Make Outline API reuse the Path when generating.
    private var cachedRrectPath: Path? = null // for temporary allocation in rounded rects

    /**
     * The outline Path when a non-conforming (rect or symmetric rounded rect) Outline
     * is used. This Path is necessary when [usePathForClip] is true to indicate the
     * Path to clip in [clipPath].
     */
    private var outlinePath: Path? = null

    /**
     * True when there's been an update that caused a change in the path and the Outline
     * has to be reevaluated.
     */
    private var cacheIsDirty = false

    /**
     * True when Outline cannot clip the content and the path should be used instead.
     * This is when an asymmetric rounded rect or general Path is used in the outline.
     * This is false when a Rect or a symmetric RoundRect is used in the outline.
     */
    private var usePathForClip = false

    /**
     * Scratch path used for manually clipping in software backed canvases
     */
    private var tmpPath: Path? = null

    /**
     * Scratch [RoundRect] used for manually clipping round rects in software backed canvases
     */
    private var tmpRoundRect: RoundRect? = null

    /**
     * Radius value used for symmetric rounded shapes. For rectangular or path based outlines
     * this value is 0f
     */
    private var roundedCornerRadius: Float = 0f

    /**
     * Returns the Android Outline to be used in the layer.
     */
    val outline: AndroidOutline?
        get() {
            updateCache()
            return if (!outlineNeeded || !isSupportedOutline) null else cachedOutline
        }

    /**
     * Determines if the particular outline shape or path supports clipping.
     * True for rect or symmetrical round rects.
     * This method is used to determine if the framework can handle clipping to the outline
     * for a particular shape. If not, then the clipped path must be applied directly to the canvas.
     */
    val outlineClipSupported: Boolean
        get() = !usePathForClip

    /**
     * Returns the path used to manually clip regardless if the layer supports clipping or not.
     * In some cases (i.e. software rendering) clipping must be done manually.
     * Consumers should query whether or not the layer will handle clipping with
     * [outlineClipSupported] first before applying the clip manually.
     * Or when rendering in software, the clip path provided here must always be clipped manually.
     */
    val clipPath: Path?
        get() {
            updateCache()
            return outlinePath
        }

    /**
     * Returns the top left offset for a rectangular, or rounded rect outline (regardless if it
     * is symmetric or asymmetric)
     * For path based outlines this returns [Offset.Zero]
     */
    private var rectTopLeft: Offset = Offset.Zero

    /**
     * Returns the size for a rectangular, or rounded rect outline (regardless if it
     * is symmetric or asymmetric)
     * For path based outlines this returns [Size.Zero]
     */
    private var rectSize: Size = Size.Zero

    /**
     * True when we are going to clip or have a non-zero elevation for shadows.
     */
    private var outlineNeeded = false

    private var layoutDirection = LayoutDirection.Ltr

    private var tmpTouchPointPath: Path? = null
    private var tmpOpPath: Path? = null
    private var calculatedOutline: Outline? = null

    /**
     * Updates the values of the outline. Returns `true` when the shape has changed.
     */
    fun update(
        shape: Shape,
        alpha: Float,
        clipToOutline: Boolean,
        elevation: Float,
        layoutDirection: LayoutDirection,
        density: Density
    ): Boolean {
        cachedOutline.alpha = alpha
        val shapeChanged = this.shape != shape
        if (shapeChanged) {
            this.shape = shape
            cacheIsDirty = true
        }
        val outlineNeeded = clipToOutline || elevation > 0f
        if (this.outlineNeeded != outlineNeeded) {
            this.outlineNeeded = outlineNeeded
            cacheIsDirty = true
        }
        if (this.layoutDirection != layoutDirection) {
            this.layoutDirection = layoutDirection
            cacheIsDirty = true
        }
        if (this.density != density) {
            this.density = density
            cacheIsDirty = true
        }
        return shapeChanged
    }

    /**
     * Returns true if there is a outline and [position] is outside the outline.
     */
    fun isInOutline(position: Offset): Boolean {
        if (!outlineNeeded) {
            return true
        }
        val outline = calculatedOutline ?: return true

        return isInOutline(outline, position.x, position.y, tmpTouchPointPath, tmpOpPath)
    }

    /**
     * Manually applies the clip to the provided canvas based on the given outline.
     * This is used in scenarios where clipping must be applied manually either because
     * the outline cannot be clipped automatically for specific shapes or if the
     * layer is being rendered in software
     */
    fun clipToOutline(canvas: Canvas) {
        // If we have a clip path that means we are clipping to an arbitrary path or
        // a rounded rect with non-uniform corner radii
        val targetPath = clipPath
        if (targetPath != null) {
            canvas.clipPath(targetPath)
        } else {
            // If we have a non-zero radius, that means we are clipping to a symmetrical
            // rounded rectangle.
            // Canvas does not include a clipRoundRect API so create a path with the round rect
            // and clip to the given path/
            if (roundedCornerRadius > 0f) {
                var roundRectClipPath = tmpPath
                var roundRect = tmpRoundRect
                if (roundRectClipPath == null ||
                    !roundRect.isSameBounds(rectTopLeft, rectSize, roundedCornerRadius)) {
                    roundRect = RoundRect(
                        left = rectTopLeft.x,
                        top = rectTopLeft.y,
                        right = rectTopLeft.x + rectSize.width,
                        bottom = rectTopLeft.y + rectSize.height,
                        cornerRadius = CornerRadius(roundedCornerRadius)
                    )
                    if (roundRectClipPath == null) {
                        roundRectClipPath = Path()
                    } else {
                        roundRectClipPath.reset()
                    }
                    roundRectClipPath.addRoundRect(roundRect)
                    tmpRoundRect = roundRect
                    tmpPath = roundRectClipPath
                }
                canvas.clipPath(roundRectClipPath)
            } else {
                // ... otherwise, just clip to the bounds of the rect
                canvas.clipRect(
                    left = rectTopLeft.x,
                    top = rectTopLeft.y,
                    right = rectTopLeft.x + rectSize.width,
                    bottom = rectTopLeft.y + rectSize.height,
                )
            }
        }
    }

    /**
     * Updates the size.
     */
    fun update(size: Size) {
        if (this.size != size) {
            this.size = size
            cacheIsDirty = true
        }
    }

    private fun updateCache() {
        if (cacheIsDirty) {
            rectTopLeft = Offset.Zero
            rectSize = size
            roundedCornerRadius = 0f
            outlinePath = null
            cacheIsDirty = false
            usePathForClip = false
            if (outlineNeeded && size.width > 0.0f && size.height > 0.0f) {
                // Always assume the outline type is supported
                // The methods to configure the outline will determine/update the flag
                // if it not supported on the API level
                isSupportedOutline = true
                val outline = shape.createOutline(size, layoutDirection, density)
                calculatedOutline = outline
                when (outline) {
                    is Outline.Rectangle -> updateCacheWithRect(outline.rect)
                    is Outline.Rounded -> updateCacheWithRoundRect(outline.roundRect)
                    is Outline.Generic -> updateCacheWithPath(outline.path)
                }
            } else {
                cachedOutline.setEmpty()
            }
        }
    }

    private fun updateCacheWithRect(rect: Rect) {
        rectTopLeft = Offset(rect.left, rect.top)
        rectSize = Size(rect.width, rect.height)
        cachedOutline.setRect(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()
        )
    }

    private fun updateCacheWithRoundRect(roundRect: RoundRect) {
        val radius = roundRect.topLeftCornerRadius.x
        rectTopLeft = Offset(roundRect.left, roundRect.top)
        rectSize = Size(roundRect.width, roundRect.height)
        if (roundRect.isSimple) {
            cachedOutline.setRoundRect(
                roundRect.left.roundToInt(),
                roundRect.top.roundToInt(),
                roundRect.right.roundToInt(),
                roundRect.bottom.roundToInt(),
                radius
            )
            roundedCornerRadius = radius
        } else {
            val path = cachedRrectPath ?: Path().also { cachedRrectPath = it }
            path.reset()
            path.addRoundRect(roundRect)
            updateCacheWithPath(path)
        }
    }

    @Suppress("deprecation")
    private fun updateCacheWithPath(composePath: Path) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P || composePath.isConvex) {
            // TODO(mount): Use setPath() for R+ when available.
            cachedOutline.setConvexPath(composePath.asAndroidPath())
            usePathForClip = !cachedOutline.canClip()
        } else {
            isSupportedOutline = false // Concave outlines are not supported on older API levels
            cachedOutline.setEmpty()
            usePathForClip = true
        }
        outlinePath = composePath
    }

    /**
     * Helper method to see if the RoundRect has the same bounds as the offset as well as the same
     * corner radius. If the RoundRect does not have symmetrical corner radii this method always
     * returns false
     */
    private fun RoundRect?.isSameBounds(offset: Offset, size: Size, radius: Float): Boolean {
        if (this == null || !isSimple) {
            return false
        }
        return left == offset.x &&
            top == offset.y &&
            right == (offset.x + size.width) &&
            bottom == (offset.y + size.height) &&
            topLeftCornerRadius.x == radius
    }
}
