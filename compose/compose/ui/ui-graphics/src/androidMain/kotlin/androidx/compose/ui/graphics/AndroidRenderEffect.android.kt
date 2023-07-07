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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * Convert the [android.graphics.RenderEffect] instance into a Compose-compatible [RenderEffect]
 */
fun android.graphics.RenderEffect.asComposeRenderEffect(): RenderEffect =
    AndroidRenderEffect(this)

@Immutable
actual sealed class RenderEffect {

    private var internalRenderEffect: android.graphics.RenderEffect? = null

    /**
     * Obtain a [android.graphics.RenderEffect] from the compose [RenderEffect]
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun asAndroidRenderEffect(): android.graphics.RenderEffect =
        internalRenderEffect ?: createRenderEffect().also { internalRenderEffect = it }

    @RequiresApi(Build.VERSION_CODES.S)
    protected abstract fun createRenderEffect(): android.graphics.RenderEffect

    actual open fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@Immutable
internal class AndroidRenderEffect(
    val androidRenderEffect: android.graphics.RenderEffect
) : RenderEffect() {
    override fun createRenderEffect(): android.graphics.RenderEffect = androidRenderEffect
}

@Immutable
actual class BlurEffect actual constructor(
    private val renderEffect: RenderEffect?,
    private val radiusX: Float,
    private val radiusY: Float,
    private val edgeTreatment: TileMode
) : RenderEffect() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun createRenderEffect(): android.graphics.RenderEffect =
        RenderEffectVerificationHelper.createBlurEffect(
            renderEffect,
            radiusX,
            radiusY,
            edgeTreatment
        )

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
}

@Immutable
actual class OffsetEffect actual constructor(
    private val renderEffect: RenderEffect?,
    private val offset: Offset
) : RenderEffect() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun createRenderEffect(): android.graphics.RenderEffect =
        RenderEffectVerificationHelper.createOffsetEffect(renderEffect, offset)

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

@RequiresApi(Build.VERSION_CODES.S)
private object RenderEffectVerificationHelper {

    @androidx.annotation.DoNotInline
    fun createBlurEffect(
        inputRenderEffect: RenderEffect?,
        radiusX: Float,
        radiusY: Float,
        edgeTreatment: TileMode
    ): android.graphics.RenderEffect =
        if (inputRenderEffect == null) {
            android.graphics.RenderEffect.createBlurEffect(
                radiusX,
                radiusY,
                edgeTreatment.toAndroidTileMode()
            )
        } else {
            android.graphics.RenderEffect.createBlurEffect(
                radiusX,
                radiusY,
                inputRenderEffect.asAndroidRenderEffect(),
                edgeTreatment.toAndroidTileMode()
            )
        }

    @androidx.annotation.DoNotInline
    fun createOffsetEffect(
        inputRenderEffect: RenderEffect?,
        offset: Offset
    ): android.graphics.RenderEffect =
        if (inputRenderEffect == null) {
            android.graphics.RenderEffect.createOffsetEffect(offset.x, offset.y)
        } else {
            android.graphics.RenderEffect.createOffsetEffect(
                offset.x,
                offset.y,
                inputRenderEffect.asAndroidRenderEffect()
            )
        }
}
