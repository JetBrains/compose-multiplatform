package org.jetbrains.compose.resources

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize

/**
 * Creates a drawing environment that directs its drawing commands to an [ImageBitmap]
 * which can be drawn directly in another [DrawScope] instance. This is useful to cache
 * complicated drawing commands across frames especially if the content has not changed.
 * Additionally some drawing operations such as rendering paths are done purely in
 * software so it is beneficial to cache the result and render the contents
 * directly through a texture as done by [DrawScope.drawImage]
 */
internal class DrawCache {

    @PublishedApi internal var mCachedImage: ImageBitmap? = null
    private var cachedCanvas: Canvas? = null
    private var scopeDensity: Density? = null
    private var layoutDirection: LayoutDirection = LayoutDirection.Ltr
    private var size: IntSize = IntSize.Zero
    private var config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

    private val cacheScope = CanvasDrawScope()

    /**
     * Draw the contents of the lambda with receiver scope into an [ImageBitmap] with the provided
     * size. If the same size is provided across calls, the same [ImageBitmap] instance is
     * re-used and the contents are cleared out before drawing content in it again
     */
    fun drawCachedImage(
        config: ImageBitmapConfig,
        size: IntSize,
        density: Density,
        layoutDirection: LayoutDirection,
        block: DrawScope.() -> Unit
    ) {
        this.scopeDensity = density
        this.layoutDirection = layoutDirection
        var targetImage = mCachedImage
        var targetCanvas = cachedCanvas
        if (targetImage == null ||
            targetCanvas == null ||
            size.width > targetImage.width ||
            size.height > targetImage.height ||
            this.config != config
        ) {
            targetImage = ImageBitmap(size.width, size.height, config = config)
            targetCanvas = Canvas(targetImage)

            mCachedImage = targetImage
            cachedCanvas = targetCanvas
            this.config = config
        }
        this.size = size
        cacheScope.draw(density, layoutDirection, targetCanvas, size.toSize()) {
            clear()
            block()
        }
        targetImage.prepareToDraw()
    }

    /**
     * Draw the cached content into the provided [DrawScope] instance
     */
    fun drawInto(
        target: DrawScope,
        alpha: Float = 1.0f,
        colorFilter: ColorFilter? = null
    ) {
        val targetImage = mCachedImage
        check(targetImage != null) {
            "drawCachedImage must be invoked first before attempting to draw the result " +
                    "into another destination"
        }
        target.drawImage(targetImage, srcSize = size, alpha = alpha, colorFilter = colorFilter)
    }

    /**
     * Helper method to clear contents of the draw environment from the given bounds of the
     * DrawScope
     */
    private fun DrawScope.clear() {
        drawRect(color = Color.Black, blendMode = BlendMode.Clear)
    }
}