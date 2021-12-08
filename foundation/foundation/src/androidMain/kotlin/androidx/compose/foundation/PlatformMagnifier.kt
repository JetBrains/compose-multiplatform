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

package androidx.compose.foundation

import android.os.Build
import android.view.View
import android.widget.Magnifier
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Stable
internal interface PlatformMagnifierFactory {

    /**
     * If true, passing a different zoom level to [PlatformMagnifier.update] on the
     * [PlatformMagnifier] returned from [create] will actually update the magnifier.
     * If false, a new [PlatformMagnifier] must be created to use a different zoom level.
     */
    val canUpdateZoom: Boolean

    @OptIn(ExperimentalFoundationApi::class)
    fun create(
        style: MagnifierStyle,
        view: View,
        density: Density,
        initialZoom: Float
    ): PlatformMagnifier

    companion object {
        @Stable
        fun getForCurrentPlatform(): PlatformMagnifierFactory =
            when {
                !isPlatformMagnifierSupported() -> {
                    throw UnsupportedOperationException(
                        "Magnifier is only supported on API level 28 and higher."
                    )
                }
                Build.VERSION.SDK_INT == 28 -> PlatformMagnifierFactoryApi28Impl
                else -> PlatformMagnifierFactoryApi29Impl
            }
    }
}

/**
 * Abstraction around the framework [Magnifier] class, for testing.
 */
internal interface PlatformMagnifier {

    /** Returns the actual size of the magnifier widget, even if not specified at creation. */
    val size: IntSize

    /** Causes the magnifier to re-copy the magnified pixels. Wraps [Magnifier.update]. */
    fun updateContent()

    /**
     * Sets the properties on a [Magnifier] instance that can be updated without recreating the
     * magnifier (e.g. [Magnifier.setZoom]) and [shows][Magnifier.show] it.
     */
    fun update(
        sourceCenter: Offset,
        magnifierCenter: Offset,
        zoom: Float
    )

    /** Wraps [Magnifier.dismiss]. */
    fun dismiss()
}

@RequiresApi(28)
internal object PlatformMagnifierFactoryApi28Impl : PlatformMagnifierFactory {
    override val canUpdateZoom: Boolean = false

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalFoundationApi::class)
    override fun create(
        style: MagnifierStyle,
        view: View,
        density: Density,
        initialZoom: Float
    ): PlatformMagnifierImpl = PlatformMagnifierImpl(Magnifier(view))

    @RequiresApi(28)
    open class PlatformMagnifierImpl(val magnifier: Magnifier) : PlatformMagnifier {

        override val size: IntSize
            get() = IntSize(magnifier.width, magnifier.height)

        override fun updateContent() {
            magnifier.update()
        }

        override fun update(
            sourceCenter: Offset,
            magnifierCenter: Offset,
            zoom: Float
        ) {
            magnifier.show(sourceCenter.x, sourceCenter.y)
        }

        override fun dismiss() {
            magnifier.dismiss()
        }
    }
}

@RequiresApi(29)
internal object PlatformMagnifierFactoryApi29Impl : PlatformMagnifierFactory {
    override val canUpdateZoom: Boolean = true

    @OptIn(ExperimentalFoundationApi::class)
    override fun create(
        style: MagnifierStyle,
        view: View,
        density: Density,
        initialZoom: Float
    ): PlatformMagnifierImpl {
        with(density) {
            // TODO write test for this branch
            if (style == MagnifierStyle.TextDefault) {
                // This deprecated constructor is the only public API to create a Magnifier that
                // uses the system text magnifier defaults.
                @Suppress("DEPRECATION")
                return PlatformMagnifierImpl(Magnifier(view))
            }

            val size = style.size.toSize()
            val cornerRadius = style.cornerRadius.toPx()
            val elevation = style.elevation.toPx()

            // When Builder properties are not specified, the widget uses different defaults than it
            // does for the non-builder constructor above.
            val magnifier = Magnifier.Builder(view).run {
                if (size.isSpecified) setSize(size.width.roundToInt(), size.height.roundToInt())
                if (!cornerRadius.isNaN()) setCornerRadius(cornerRadius)
                if (!elevation.isNaN()) setElevation(elevation)
                if (!initialZoom.isNaN()) setInitialZoom(initialZoom)
                setClippingEnabled(style.clippingEnabled)
                // TODO(b/202451044) Support setting fisheye style.
                build()
            }

            return PlatformMagnifierImpl(magnifier)
        }
    }

    @RequiresApi(29)
    class PlatformMagnifierImpl(magnifier: Magnifier) :
        PlatformMagnifierFactoryApi28Impl.PlatformMagnifierImpl(magnifier) {

        override fun update(sourceCenter: Offset, magnifierCenter: Offset, zoom: Float) {
            if (!zoom.isNaN()) magnifier.zoom = zoom

            if (magnifierCenter.isSpecified) {
                magnifier.show(
                    sourceCenter.x, sourceCenter.y,
                    magnifierCenter.x, magnifierCenter.y
                )
            } else {
                // This overload places the magnifier at a default offset relative to the source.
                magnifier.show(sourceCenter.x, sourceCenter.y)
            }
        }
    }
}
