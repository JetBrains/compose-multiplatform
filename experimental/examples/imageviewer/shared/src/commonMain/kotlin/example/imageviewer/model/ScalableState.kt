package example.imageviewer.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified

/**
 * Encapsulate all transformations about showing some target (an image, relative to its center)
 * scaled and shifted in some area (a window, relative to its center)
 */
class ScalableState {
    val scaleLimits = 0.5f..10f

    /**
     * Offset of the camera before scaling (an offset in pixels in the area coordinate system)
     */
    var offset by mutableStateOf(Offset.Zero)
        private set
    var scale by mutableStateOf(1f)
        private set

    private var areaSize: Size = Size.Unspecified
    private var targetSize: Size = Size.Zero

    private var offsetXLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
    private var offsetYLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY

    /**
     * Limit the target center position, so:
     * - if the size of the target is less than area,
     *   the center of the target is bound to the center of the area
     * - if the size of the target is greater, then limit the center of it,
     *   so the target will be always in the area
     */
    fun limitTargetInsideArea(
        areaSize: Size,
        targetSize: Size,
    ) {
        this.areaSize = areaSize
        this.targetSize = targetSize
        applyLimits()
    }

    private fun applyLimits() {
        if (targetSize.isSpecified && areaSize.isSpecified) {
            offsetXLimits = centerLimits(targetSize.width * scale, areaSize.width)
            offsetYLimits = centerLimits(targetSize.height * scale, areaSize.height)
            offset = Offset(
                offset.x.coerceIn(offsetXLimits),
                offset.y.coerceIn(offsetYLimits),
            )
        } else {
            offsetXLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
            offsetYLimits = Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY
        }
    }

    private fun centerLimits(imageSize: Float, areaSize: Float): ClosedFloatingPointRange<Float> {
        val areaCenter = areaSize / 2
        val imageCenter = imageSize / 2
        val extra = (imageCenter - areaCenter).coerceAtLeast(0f)
        return -extra / 2..extra / 2
    }

    fun addPan(pan: Offset) {
        offset += pan
        applyLimits()
    }

    /**
     * @param focus on which point the camera is focused in the area coordinate system.
     * After we apply the new scale, the camera should be focused on the same point in
     * the target coordinate system.
     */
    fun addScale(scaleMultiplier: Float, focus: Offset = Offset.Zero) {
        setScale(scale * scaleMultiplier, focus)
    }

    fun setScale(scale: Float, focus: Offset = Offset.Zero) {
        val newScale = scale.coerceIn(scaleLimits)
        val focusInTargetSystem = (focus - offset) / this.scale
        // calculate newOffset from this equation:
        // focusInTargetSystem = (focus - newOffset) / newScale
        offset = focus - focusInTargetSystem * newScale
        this.scale = newScale
        applyLimits()
    }
}
