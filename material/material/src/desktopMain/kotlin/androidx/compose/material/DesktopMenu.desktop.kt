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

package androidx.compose.material

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.contextMenuOpenDetector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.rememberCursorPositionProvider
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import java.awt.event.KeyEvent

/**
 * A Material Design [dropdown menu](https://material.io/components/menus#dropdown-menu).
 *
 * A [DropdownMenu] behaves similarly to a [Popup], and will use the position of the parent layout
 * to position itself on screen. Commonly a [DropdownMenu] will be placed in a [Box] with a sibling
 * that will be used as the 'anchor'. Note that a [DropdownMenu] by itself will not take up any
 * space in a layout, as the menu is displayed in a separate window, on top of other content.
 *
 * The [content] of a [DropdownMenu] will typically be [DropdownMenuItem]s, as well as custom
 * content. Using [DropdownMenuItem]s will result in a menu that matches the Material
 * specification for menus. Also note that the [content] is placed inside a scrollable [Column],
 * so using a [LazyColumn] as the root layout inside [content] is unsupported.
 *
 * [onDismissRequest] will be called when the menu should close - for example when there is a
 * tap outside the menu, or when the back key is pressed.
 *
 * [DropdownMenu] changes its positioning depending on the available space, always trying to be
 * fully visible. It will try to expand horizontally, depending on layout direction, to the end of
 * its parent, then to the start of its parent, and then screen end-aligned. Vertically, it will
 * try to expand to the bottom of its parent, then from the top of its parent, and then screen
 * top-aligned. An [offset] can be provided to adjust the positioning of the menu for cases when
 * the layout bounds of its parent do not coincide with its visual bounds. Note the offset will
 * be applied in the direction in which the menu will decide to expand.
 *
 * Example usage:
 * @sample androidx.compose.material.samples.MenuSample
 *
 * @param expanded Whether the menu is currently open and visible to the user
 * @param onDismissRequest Called when the user requests to dismiss the menu, such as by
 * tapping outside the menu's bounds
 * @param offset [DpOffset] to be added to the position of the menu
 */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    focusable: Boolean = true,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        // The original [DropdownMenuPositionProvider] is not yet suitable for large screen devices,
        // so we need to make additional checks and adjust the position of the [DropdownMenu] to
        // avoid content being cut off if the [DropdownMenu] contains too many items.
        // See: https://github.com/JetBrains/compose-jb/issues/1388
        val popupPositionProvider = DesktopDropdownMenuPositionProvider(
            offset,
            density
        ) { parentBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
        }

        OpenDropdownMenu(
            expandedStates = expandedStates,
            popupPositionProvider = popupPositionProvider,
            transformOriginState = transformOriginState,
            onDismissRequest = onDismissRequest,
            focusable = focusable,
            modifier = modifier,
            content = content
        )
    }
}

/**
 * A variant of a dropdown menu that accepts a [DropdownMenuState] instead of directly using the
 * mouse position.
 *
 * Typically, it should be combined with [Modifier.contextMenuOpenDetector] via state-hoisting.
 *
 * @param state The open/closed state of the menu.
 * @param onDismissRequest Called when the user requests to dismiss the menu, such as by
 * tapping outside the menu's bounds
 *
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DropdownMenu(
    state: DropdownMenuState,
    onDismissRequest: () -> Unit = { state.status = DropdownMenuState.Status.Closed },
    focusable: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val status = state.status
    var position: Offset? by remember { mutableStateOf(null) }
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = status is DropdownMenuState.Status.Open

    // Whenever we are asked to open the popup, remember the position
    if (status is DropdownMenuState.Status.Open){
        position = status.position
    }

    if (expandedStates.currentState || expandedStates.targetState) {
        OpenDropdownMenu(
            expandedStates = expandedStates,
            popupPositionProvider = rememberPopupPositionProviderAtPosition(position!!),
            onDismissRequest = onDismissRequest,
            focusable = focusable,
            modifier = modifier,
            content = content
        )
    }
}

/**
 * The implementation of a [DropdownMenu] in its open state.
 */
@Composable
private fun OpenDropdownMenu(
    expandedStates: MutableTransitionState<Boolean>,
    popupPositionProvider: PopupPositionProvider,
    transformOriginState: MutableState<TransformOrigin> =
        remember { mutableStateOf(TransformOrigin.Center) },
    onDismissRequest: () -> Unit,
    focusable: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
){
    var focusManager: FocusManager? by mutableStateOf(null)
    var inputModeManager: InputModeManager? by mutableStateOf(null)
    Popup(
        focusable = focusable,
        onDismissRequest = onDismissRequest,
        popupPositionProvider = popupPositionProvider,
        onKeyEvent = {
            handlePopupOnKeyEvent(it, onDismissRequest, focusManager!!, inputModeManager!!)
        },
    ) {
        focusManager = LocalFocusManager.current
        inputModeManager = LocalInputModeManager.current

        DropdownMenuContent(
            expandedStates = expandedStates,
            transformOriginState = transformOriginState,
            modifier = modifier,
            content = content
        )
    }
}

/**
 * A dropdown menu item, as defined by the Material Design spec.
 *
 * Example usage:
 * @sample androidx.compose.material.samples.MenuSample
 *
 * @param onClick Called when the menu item was clicked
 * @param modifier The modifier to be applied to the menu item
 * @param enabled Controls the enabled state of the menu item - when `false`, the menu item
 * will not be clickable and [onClick] will not be invoked
 * @param contentPadding the padding applied to the content of this menu item
 * @param interactionSource the [MutableInteractionSource] representing the different [Interaction]s
 * present on this DropdownMenuItem. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to read the [MutableInteractionSource] and customize
 * the appearance / behavior of this DropdownMenuItem in different [Interaction]s.
 */
@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    DropdownMenuItemContent(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun handlePopupOnKeyEvent(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    onDismissRequest: () -> Unit,
    focusManager: FocusManager,
    inputModeManager: InputModeManager
): Boolean {
    return if (keyEvent.type == KeyEventType.KeyDown && keyEvent.awtEventOrNull?.keyCode == KeyEvent.VK_ESCAPE) {
        onDismissRequest()
        true
    } else if (keyEvent.type == KeyEventType.KeyDown) {
        when {
            keyEvent.isDirectionDown -> {
                inputModeManager.requestInputMode(InputMode.Keyboard)
                focusManager.moveFocus(FocusDirection.Next)
                true
            }
            keyEvent.isDirectionUp -> {
                inputModeManager.requestInputMode(InputMode.Keyboard)
                focusManager.moveFocus(FocusDirection.Previous)
                true
            }
            else -> false
        }
    } else {
        false
    }
}

/**
 * A [CursorDropdownMenu] behaves similarly to [Popup] and will use the current position of the mouse
 * cursor to position itself on screen.
 *
 * The [content] of a [CursorDropdownMenu] will typically be [DropdownMenuItem]s, as well as custom
 * content. Using [DropdownMenuItem]s will result in a menu that matches the Material
 * specification for menus.
 *
 * @param expanded Whether the menu is currently open and visible to the user
 * @param onDismissRequest Called when the user requests to dismiss the menu, such as by
 * tapping outside the menu's bounds
 */
@Composable
fun CursorDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    focusable: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        OpenDropdownMenu(
            expandedStates = expandedStates,
            popupPositionProvider = rememberCursorPositionProvider(),
            onDismissRequest = onDismissRequest,
            focusable = focusable,
            modifier = modifier,
            content = content
        )
    }
}

/**
 * Represents the open/closed state of a dropdown menu.
 */
@Stable
class DropdownMenuState(initialStatus: Status = Status.Closed) {

    /**
     * The current status of the menu.
     */
    var status: Status by mutableStateOf(initialStatus)

    @Immutable
    sealed class Status {

        class Open(val position: Offset) : Status() {

            override fun equals(other: Any?): Boolean {
                if (this === other)
                    return true

                if (other !is Open)
                    return false

                if (position != other.position)
                    return false

                return true
            }

            override fun hashCode(): Int {
                return position.hashCode()
            }

            override fun toString(): String {
                return "Open(position=$position)"
            }
        }

        object Closed : Status()

    }

}

/**
 * A [Modifier] that detects events that should typically open a context menu (mouse right-clicks)
 * and modify the given [DropdownMenuState] accordingly.
 */
@ExperimentalMaterialApi
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.contextMenuOpenDetector(
    state: DropdownMenuState,
    enabled: Boolean = true,
): Modifier {
    return if (enabled) {
        this.contextMenuOpenDetector(
            key = state,
            enabled = enabled && (state.status is DropdownMenuState.Status.Closed)
        ) { pointerPosition ->
            state.status = DropdownMenuState.Status.Open(pointerPosition)
        }
    } else {
        this
    }
}

@Immutable
internal data class DesktopDropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(toRight, toLeft, toDisplayRight)
        } else {
            sequenceOf(toLeft, toRight, toDisplayLeft)
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        var y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        // Desktop specific vertical position checking
        val aboveAnchor = anchorBounds.top + contentOffsetY
        val belowAnchor = windowSize.height - anchorBounds.bottom - contentOffsetY

        if (belowAnchor >= aboveAnchor) {
            y = anchorBounds.bottom + contentOffsetY
        }

        if (y + popupContentSize.height > windowSize.height) {
            y = windowSize.height - popupContentSize.height
        }

        y = y.coerceAtLeast(0)

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}

