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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
@Composable
fun rememberWindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: DpSize = DpSize(800.dp, 600.dp),
): WindowState = rememberSaveable(saver = WindowStateImpl.Saver(position)) {
    WindowStateImpl(
        placement,
        isMinimized,
        position,
        size
    )
}

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
@Suppress("DEPRECATION")
@Composable
@Deprecated("Use rememberWindowState which accepts DpSize")
fun rememberWindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: WindowSize
): WindowState = rememberSaveable(saver = WindowStateImpl.Saver(position)) {
    WindowStateImpl(
        placement,
        isMinimized,
        position,
        DpSize(size.width, size.height)
    )
}

/**
 * Creates a [WindowState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param width the initial value for width of [WindowState.size]
 * @param height the initial value for height of  [WindowState.size]
 */
@Composable
fun rememberWindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    width: Dp = 800.dp,
    height: Dp = 600.dp
): WindowState = rememberSaveable(saver = WindowStateImpl.Saver(position)) {
    WindowStateImpl(
        placement,
        isMinimized,
        position,
        DpSize(width, height)
    )
}

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
fun WindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: DpSize = DpSize(800.dp, 600.dp)
): WindowState = WindowStateImpl(
    placement, isMinimized, position, size
)

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param size the initial value for [WindowState.size]
 */
@Deprecated("Use WindowState which accepts DpSize")
@Suppress("DEPRECATION")
fun WindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    size: WindowSize
): WindowState = WindowStateImpl(
    placement, isMinimized, position, DpSize(size.width, size.height)
)

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 *
 * @param placement the initial value for [WindowState.placement]
 * @param isMinimized the initial value for [WindowState.isMinimized]
 * @param position the initial value for [WindowState.position]
 * @param width the initial value for width of [WindowState.size]
 * @param height the initial value for height of  [WindowState.size]
 */
fun WindowState(
    placement: WindowPlacement = WindowPlacement.Floating,
    isMinimized: Boolean = false,
    position: WindowPosition = WindowPosition.PlatformDefault,
    width: Dp = 800.dp,
    height: Dp = 600.dp
): WindowState = WindowStateImpl(
    placement, isMinimized, position, DpSize(width, height)
)

/**
 * A state object that can be hoisted to control and observe window attributes
 * (size/position/state).
 */
interface WindowState {
    /**
     * Describes how the window is placed on the screen.
     */
    var placement: WindowPlacement

    /**
     * `true` if the window is minimized.
     */
    var isMinimized: Boolean

    /**
     * The current position of the window. If the position is not specified
     * ([WindowPosition.isSpecified] is false), the position will be set to absolute values
     * [WindowPosition.Absolute] when the window appears on the screen.
     */
    var position: WindowPosition

    /**
     * The current size of the window.
     *
     * If the size is not specified
     * ([DpSize.width.isSpecified] or [DpSize.height.isSpecified] is false), the size will be set
     * to absolute values
     * ([Dp.isSpecified] is true) when the window appears on the screen.
     *
     * Unspecified can be only width, only height, or both. If, for example, window contains some
     * text and we use size=DpSize(300.dp, Dp.Unspecified) then the width will be exactly
     * 300.dp, but the height will be such that all the text will fit.
     */
    var size: DpSize
}

private class WindowStateImpl(
    placement: WindowPlacement,
    isMinimized: Boolean,
    position: WindowPosition,
    size: DpSize
) : WindowState {
    override var placement by mutableStateOf(placement)
    override var isMinimized by mutableStateOf(isMinimized)
    override var position by mutableStateOf(position)
    override var size by mutableStateOf(size)

    companion object {
        /**
         * The default [Saver] implementation for [WindowStateImpl].
         */
        fun Saver(unspecifiedPosition: WindowPosition) = listSaver<WindowState, Any>(
            save = {
                listOf(
                    it.placement.ordinal,
                    it.isMinimized,
                    it.position.isSpecified,
                    it.position.x.value,
                    it.position.y.value,
                    it.size.width.value,
                    it.size.height.value,
                )
            },
            restore = { state ->
                WindowStateImpl(
                    placement = WindowPlacement.values()[state[0] as Int],
                    isMinimized = state[1] as Boolean,
                    position = if (state[2] as Boolean) {
                        WindowPosition((state[3] as Float).dp, (state[4] as Float).dp)
                    } else {
                        unspecifiedPosition
                    },
                    size = DpSize((state[5] as Float).dp, (state[6] as Float).dp),
                )
            }
        )
    }
}