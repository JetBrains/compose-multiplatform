/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import org.jetbrains.skia.ImageFilter

/**
 * Convert the [ImageFilter] instance into a Compose-compatible [RenderEffect]
 */
fun ImageFilter.asComposeRenderEffect(): RenderEffect =
    DesktopRenderEffect(this)

/**
 * Intermediate rendering step used to render drawing commands with a corresponding
 * visual effect. A [RenderEffect] can be configured on a [GraphicsLayerScope]
 * and will be applied when drawn.
 */
@Immutable
actual sealed class RenderEffect actual constructor() {

    private var internalImageFilter: ImageFilter? = null

    @Deprecated("Use asSkiaImageFilter()", replaceWith = ReplaceWith("asSkiaImageFilter()"))
    fun asDesktopImageFilter(): ImageFilter = asSkiaImageFilter()

    fun asSkiaImageFilter(): ImageFilter =
        internalImageFilter ?: createImageFilter().also { internalImageFilter = it }

    protected abstract fun createImageFilter(): ImageFilter

    /**
     * Capability query to determine if the particular platform supports the [RenderEffect]. Not
     * all platforms support all render effects
     */
    actual open fun isSupported(): Boolean = true
}

@Immutable
internal class DesktopRenderEffect(
    val imageFilter: ImageFilter
) : RenderEffect() {
    override fun createImageFilter(): ImageFilter = imageFilter
}

@Immutable
actual class BlurEffect actual constructor(
    private val renderEffect: RenderEffect?,
    private val radiusX: Float,
    private val radiusY: Float,
    private val edgeTreatment: TileMode
) : RenderEffect() {

    override fun createImageFilter(): ImageFilter =
        if (renderEffect == null) {
            ImageFilter.makeBlur(
                convertRadiusToSigma(radiusX),
                convertRadiusToSigma(radiusY),
                edgeTreatment.toSkiaTileMode()
            )
        } else {
            ImageFilter.makeBlur(
                convertRadiusToSigma(radiusX),
                convertRadiusToSigma(radiusY),
                edgeTreatment.toSkiaTileMode(),
                renderEffect.asSkiaImageFilter(),
                null
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlurEffect) return false

        if (radiusX != other.radiusX) return false
        if (radiusY != other.radiusY) return false
        if (edgeTreatment != other.edgeTreatment) return false
        if (renderEffect != other.renderEffect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = renderEffect?.hashCode() ?: 0
        result = 31 * result + radiusX.hashCode()
        result = 31 * result + radiusY.hashCode()
        result = 31 * result + edgeTreatment.hashCode()
        return result
    }

    override fun toString(): String {
        return "BlurEffect(renderEffect=$renderEffect, radiusX=$radiusX, radiusY=$radiusY, " +
            "edgeTreatment=$edgeTreatment)"
    }

    companion object {

        // Constant used to convert blur radius into a corresponding sigma value
        // for the gaussian blur algorithm used within SkImageFilter.
        // This constant approximates the scaling done in the software path's
        // "high quality" mode, in SkBlurMask::Blur() (1 / sqrt(3)).
        val BlurSigmaScale = 0.57735f

        fun convertRadiusToSigma(radius: Float) =
            if (radius > 0) {
                BlurSigmaScale * radius + 0.5f
            } else {
                0.0f
            }
    }
}

@Immutable
actual class OffsetEffect actual constructor(
    private val renderEffect: RenderEffect?,
    private val offset: Offset
) : RenderEffect() {

    override fun createImageFilter(): ImageFilter =
        ImageFilter.makeOffset(offset.x, offset.y, renderEffect?.asSkiaImageFilter(), null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OffsetEffect) return false

        if (renderEffect != other.renderEffect) return false
        if (offset != other.offset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = renderEffect?.hashCode() ?: 0
        result = 31 * result + offset.hashCode()
        return result
    }

    override fun toString(): String {
        return "OffsetEffect(renderEffect=$renderEffect, offset=$offset)"
    }
}