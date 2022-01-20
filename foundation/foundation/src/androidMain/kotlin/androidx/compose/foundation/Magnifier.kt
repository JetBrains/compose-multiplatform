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

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Magnifier
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A function on elements that are magnified with a [magnifier] modifier that returns the position
 * of the center of the magnified content in the coordinate space of the root composable.
 */
internal val MagnifierPositionInRoot =
    SemanticsPropertyKey<() -> Offset>("MagnifierPositionInRoot")

/**
 * Specifies how a [magnifier] should create the underlying [Magnifier] widget. These properties
 * should not be changed while a magnifier is showing, since the magnifier will be dismissed and
 * recreated with the new properties which will cause it to disappear for at least a frame.
 *
 * Not all magnifier features are supported on all platforms. The [isSupported] property will return
 * false for styles that cannot be fully supported on the given platform.
 *
 * @param size See [Magnifier.Builder.setSize]. Only supported on API 29+.
 * @param cornerRadius See [Magnifier.Builder.setCornerRadius]. Only supported on API 29+.
 * @param elevation See [Magnifier.Builder.setElevation]. Only supported on API 29+.
 * @param clippingEnabled See [Magnifier.Builder.setClippingEnabled]. Only supported on API 29+.
 * @param fishEyeEnabled Configures the magnifier to distort the magnification at the edges to
 * look like a fisheye lens. Not currently supported.
 */
@ExperimentalFoundationApi
@Stable
class MagnifierStyle internal constructor(
    internal val useTextDefault: Boolean,
    internal val size: DpSize,
    internal val cornerRadius: Dp,
    internal val elevation: Dp,
    internal val clippingEnabled: Boolean,
    internal val fishEyeEnabled: Boolean
) {
    @ExperimentalFoundationApi
    constructor(
        size: DpSize = DpSize.Unspecified,
        cornerRadius: Dp = Dp.Unspecified,
        elevation: Dp = Dp.Unspecified,
        clippingEnabled: Boolean = true,
        fishEyeEnabled: Boolean = false
    ) : this(
        useTextDefault = false,
        size = size,
        cornerRadius = cornerRadius,
        elevation = elevation,
        clippingEnabled = clippingEnabled,
        fishEyeEnabled = fishEyeEnabled,
    )

    /**
     * Returns true if this style is supported by this version of the platform.
     * When false is returned, it may be either because the [Magnifier] widget is not supported at
     * all because the platform is too old, or because a particular style flag (e.g.
     * [fishEyeEnabled]) is not supported on the current platform.
     * [Default] and [TextDefault] styles are supported on all platforms with SDK version 28 and
     * higher.
     */
    val isSupported: Boolean
        get() = isStyleSupported(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MagnifierStyle) return false

        if (useTextDefault != other.useTextDefault) return false
        if (size != other.size) return false
        if (cornerRadius != other.cornerRadius) return false
        if (elevation != other.elevation) return false
        if (clippingEnabled != other.clippingEnabled) return false
        if (fishEyeEnabled != other.fishEyeEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = useTextDefault.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + elevation.hashCode()
        result = 31 * result + clippingEnabled.hashCode()
        result = 31 * result + fishEyeEnabled.hashCode()
        return result
    }

    override fun toString(): String {
        return if (useTextDefault) {
            "MagnifierStyle.TextDefault"
        } else {
            "MagnifierStyle(" +
                "size=$size, " +
                "cornerRadius=$cornerRadius, " +
                "elevation=$elevation, " +
                "clippingEnabled=$clippingEnabled, " +
                "fishEyeEnabled=$fishEyeEnabled" +
                ")"
        }
    }

    companion object {
        /** A [MagnifierStyle] with all default values. */
        @ExperimentalFoundationApi
        val Default = MagnifierStyle()

        /**
         * A [MagnifierStyle] that uses the system defaults for text magnification.
         *
         * Different versions of Android may use different magnifier styles for magnifying text, so
         * using this configuration ensures that the correct style is used to match the system.
         */
        @ExperimentalFoundationApi
        val TextDefault = MagnifierStyle(
            useTextDefault = true,
            size = Default.size,
            cornerRadius = Default.cornerRadius,
            elevation = Default.elevation,
            clippingEnabled = Default.clippingEnabled,
            fishEyeEnabled = Default.fishEyeEnabled,
        )

        internal fun isStyleSupported(
            style: MagnifierStyle,
            sdkVersion: Int = Build.VERSION.SDK_INT
        ): Boolean {
            return if (!isPlatformMagnifierSupported(sdkVersion)) {
                // Older platform versions don't support magnifier at all.
                false
            } else if (style.fishEyeEnabled) {
                // TODO(b/202451044) Add fisheye support once platform APIs are exposed.
                false
            } else if (style.useTextDefault || style == Default) {
                // Default styles are always available on all platforms that support magnifier.
                true
            } else {
                // Custom styles aren't supported on API 28.
                sdkVersion >= 29
            }
        }
    }
}

/**
 * Shows a [Magnifier] widget that shows an enlarged version of the content at [sourceCenter]
 * relative to the current layout node.
 *
 * This function returns a no-op modifier on API levels below P (28), since the framework does not
 * support the [Magnifier] widget on those levels. However, even on higher API levels, not all
 * magnifier features are supported on all platforms. To check whether a given [MagnifierStyle] is
 * supported by the current platform, check the [MagnifierStyle.isSupported] property.
 *
 * This function does not allow configuration of [source bounds][Magnifier.Builder.setSourceBounds]
 * since the magnifier widget does not support constraining to the bounds of composables.
 *
 * @sample androidx.compose.foundation.samples.MagnifierSample
 *
 * @param sourceCenter The offset of the center of the magnified content. Measured in pixels from
 * the top-left of the layout node this modifier is applied to. This offset is passed to
 * [Magnifier.show].
 * @param magnifierCenter The offset of the magnifier widget itself, where the magnified content is
 * rendered over the original content. Measured in density-independent pixels from the top-left of
 * the layout node this modifier is applied to. If [unspecified][DpOffset.Unspecified], the
 * magnifier widget will be placed at a default offset relative to [sourceCenter]. The value of that
 * offset is specified by the system.
 * @param zoom See [Magnifier.setZoom]. Not supported on SDK levels < Q.
 * @param style The [MagnifierStyle] to use to configure the magnifier widget.
 * @param onSizeChanged An optional callback that will be invoked when the magnifier widget is
 * initialized to report on its actual size. This can be useful if one of the default
 * [MagnifierStyle]s is used to find out what size the system decided to use for the widget.
 */
@ExperimentalFoundationApi
fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset = { Offset.Unspecified },
    zoom: Float = Float.NaN,
    style: MagnifierStyle = MagnifierStyle.Default,
    onSizeChanged: ((DpSize) -> Unit)? = null
): Modifier = inspectable(
    // Publish inspector info even if magnification isn't supported.
    inspectorInfo = debugInspectorInfo {
        name = if (isPlatformMagnifierSupported()) "magnifier" else "magnifier (not supported)"
        properties["sourceCenter"] = sourceCenter
        properties["magnifierCenter"] = magnifierCenter
        properties["zoom"] = zoom
        properties["style"] = style
    }
) {
    if (isPlatformMagnifierSupported()) {
        magnifier(
            sourceCenter = sourceCenter,
            magnifierCenter = magnifierCenter,
            zoom = zoom,
            style = style,
            onSizeChanged = onSizeChanged,
            platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform()
        )
    } else {
        // Magnifier is only supported in >=28. So avoid doing all the work to manage the magnifier
        // state if it's not needed.
        // TODO(b/202739980) Investigate supporting Magnifier on earlier versions.
        Modifier
    }
}

/**
 * @param platformMagnifierFactory Creates a [PlatformMagnifier] whenever the configuration changes.
 */
@OptIn(ExperimentalFoundationApi::class)
// The InspectorInfo this modifier reports is for the above public overload, and intentionally
// doesn't include the platformMagnifierFactory parameter.
@SuppressLint("ModifierInspectorInfo")
@RequiresApi(28)
internal fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset,
    zoom: Float,
    style: MagnifierStyle,
    onSizeChanged: ((DpSize) -> Unit)?,
    platformMagnifierFactory: PlatformMagnifierFactory
): Modifier = composed {
    val view = LocalView.current
    val density = LocalDensity.current
    var anchorPositionInRoot: Offset by remember { mutableStateOf(Offset.Unspecified) }
    val updatedSourceCenter by rememberUpdatedState(sourceCenter)
    val updatedMagnifierCenter by rememberUpdatedState(magnifierCenter)
    val updatedZoom by rememberUpdatedState(zoom)
    val updatedOnSizeChanged by rememberUpdatedState(onSizeChanged)
    val sourceCenterInRoot by remember {
        derivedStateOf {
            val sourceCenterOffset = updatedSourceCenter(density)
            if (anchorPositionInRoot.isSpecified && sourceCenterOffset.isSpecified) {
                anchorPositionInRoot + sourceCenterOffset
            } else {
                Offset.Unspecified
            }
        }
    }
    val isMagnifierShown by remember { derivedStateOf { sourceCenterInRoot.isSpecified } }

    /**
     * Used to request that the magnifier updates its buffer when the layer is redrawn.
     */
    val onNeedsUpdate = remember {
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // On platforms >=Q, the zoom level can be updated dynamically on an existing magnifier, so if
    // the zoom changes between recompositions we don't need to recreate the magnifier. On older
    // platforms, the zoom can only be set initially, so we use the zoom itself as a key so the
    // magnifier gets recreated if it changes.
    val zoomEffectKey = if (platformMagnifierFactory.canUpdateZoom) 0f else zoom

    // centerOffset, sourceToMagnifierOffset, and zoom are not in this key list because they can be
    // updated without re-creating the Magnifier.
    LaunchedEffect(
        view,
        density,
        zoomEffectKey,
        style,
        // This is a separate key because otherwise a change from Default to TextDefault won't
        // reconfigure. Note that this checks for reference equality, not structural equality, since
        // TextDefault == Default already.
        style == MagnifierStyle.TextDefault
    ) {
        val magnifier = platformMagnifierFactory.create(style, view, density, zoom)
        var previousSize = magnifier.size.also { newSize ->
            updatedOnSizeChanged?.invoke(
                with(density) {
                    newSize.toSize().toDpSize()
                }
            )
        }

        // Ask the magnifier to do another pixel copy whenever the nearest layer is redrawn.
        onNeedsUpdate
            .onEach { magnifier.updateContent() }
            .launchIn(this)

        try {
            // Update the modifier in a snapshotFlow so it will be restarted whenever any state read
            // by the update function changes.
            snapshotFlow {
                // Once the position is set, it's never null again, so we don't need to worry about
                // dismissing the magnifier if this expression changes value.
                if (isMagnifierShown) {
                    magnifier.update(
                        sourceCenter = sourceCenterInRoot,
                        magnifierCenter = updatedMagnifierCenter(density).let {
                            if (it.isSpecified) {
                                anchorPositionInRoot + it
                            } else {
                                Offset.Unspecified
                            }
                        },
                        zoom = updatedZoom
                    )

                    magnifier.size.let { size ->
                        if (size != previousSize) {
                            previousSize = size
                            updatedOnSizeChanged?.invoke(
                                with(density) {
                                    size.toSize().toDpSize()
                                }
                            )
                        }
                    }
                } else {
                    // Can't place the magnifier at an unspecified location, so just hide it.
                    magnifier.dismiss()
                }
            }.collect()
        } finally {
            // Dismiss the magnifier whenever it needs to be recreated or it's removed from the
            // composition.
            magnifier.dismiss()
        }
    }

    return@composed this
        .onGloballyPositioned {
            // The mutable state must store the Offset, not the LocalCoordinates, because the same
            // LocalCoordinates instance may be sent to this callback multiple times, not implement
            // equals, or be stable, and so won't invalidate the snapshotFlow.
            anchorPositionInRoot = it.positionInRoot()
        }
        .drawBehind {
            // Tell the magnifier to update itself every time the layer is re-drawn.
            // Note that this won't do anything if the magnifier is showing a different layer,
            // but it handles the case where the cursor is blinking so it's better than nothing.
            onNeedsUpdate.tryEmit(Unit)
        }
        .semantics {
            this[MagnifierPositionInRoot] = { sourceCenterInRoot }
        }
}

@ChecksSdkIntAtLeast(api = 28)
internal fun isPlatformMagnifierSupported(sdkVersion: Int = Build.VERSION.SDK_INT) =
    sdkVersion >= 28
