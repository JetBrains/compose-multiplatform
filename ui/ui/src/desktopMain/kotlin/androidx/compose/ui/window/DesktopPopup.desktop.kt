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
package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwnersAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.round
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.positionChangeConsumed
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned relative to its parent, using the [alignment] and [offset].
 * The popup is visible as long as it is part of the composition hierarchy.
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 * Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 * will be subtracted from it.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val popupPositioner = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(
            alignment,
            offset
        )
    }

    Popup(
        popupPositionProvider = popupPositioner,
        isFocusable = isFocusable,
        onDismissRequest = onDismissRequest,
        content = content
    )
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @sample androidx.compose.ui.samples.PopupSample
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param isFocusable Indicates if the popup can grab the focus.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param content The content to be displayed inside the popup.
 */
@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    PopupLayout(popupPositionProvider, isFocusable, onDismissRequest, content)
}

@Composable
@Suppress("DEPRECATION") // tapGestureFilter
private fun PopupLayout(
    popupPositionProvider: PopupPositionProvider,
    isFocusable: Boolean,
    onDismissRequest: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val owners = DesktopOwnersAmbient.current
    val density = LocalDensity.current

    val parentBounds = remember { mutableStateOf(IntRect.Zero) }

    // getting parent bounds
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            parentBounds.value = IntRect(
                coordinates.localToWindow(Offset.Zero).round(),
                coordinates.size
            )
        },
        measurePolicy = { _, _ ->
            layout(0, 0) {}
        }
    )

    val parentComposition = rememberCompositionContext()
    val (owner, composition) = remember {
        val owner = DesktopOwner(owners, density)
        val composition = owner.setContent(parent = parentComposition) {
            Layout(
                content = content,
                modifier = Modifier.tapGestureFilter {
                    if (isFocusable) {
                        onDismissRequest?.invoke()
                    }
                },
                measurePolicy = { measurables, constraints ->
                    val width = constraints.maxWidth
                    val height = constraints.maxHeight

                    val windowSize = IntSize(
                        width = width,
                        height = height
                    )

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        measurables.forEach {
                            val placeable = it.measure(constraints)
                            val offset = popupPositionProvider.calculatePosition(
                                anchorBounds = parentBounds.value,
                                windowSize = windowSize,
                                layoutDirection = layoutDirection,
                                popupContentSize = IntSize(placeable.width, placeable.height)
                            )
                            placeable.place(offset.x, offset.y)
                        }
                    }
                }
            )
        }
        owner to composition
    }
    owner.density = density
    DisposableEffect(Unit) {
        onDispose {
            composition.dispose()
            owner.dispose()
        }
    }
}

/**
 * This gesture detector fires a callback when a traditional press is being released.  This is
 * generally the same thing as "onTap" or "onClick".
 *
 * [onTap] is called with the position of the last pointer to go "up".
 *
 * More specifically, it will call [onTap] if:
 * - All of the first [PointerInputChange]s it receives during the [PointerEventPass.Main] pass
 *   have unconsumed down changes, thus representing new set of pointers, none of which have had
 *   their down events consumed.
 * - The last [PointerInputChange] it receives during the [PointerEventPass.Main] pass has
 *   an unconsumed up change.
 * - While it has at least one pointer touching it, no [PointerInputChange] has had any
 *   movement consumed (as that would indicate that something in the hierarchy moved and this a
 *   press should be cancelled.
 *
 *   @param onTap Called when a tap has occurred.
 */
@Deprecated(
    "Gesture filters are deprecated. Use Modifier.clickable or Modifier.pointerInput and " +
        "detectTapGestures instead",
    replaceWith = ReplaceWith(
        """pointerInput { detectTapGestures(onTap = onTap)}""",
        "androidx.compose.ui.input.pointer.pointerInput",
        "androidx.compose.foundation.gestures.detectTapGestures"
    )
)
internal fun Modifier.tapGestureFilter(onTap: (Offset) -> Unit): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "tapGestureFilter"
        this.properties["onTap"] = onTap
    }
) {
    val filter = remember { TapGestureFilter() }
    filter.onTap = onTap
    TapPointerInputModifierImpl(filter)
}

private class TapGestureFilter : PointerInputFilter() {
    /**
     * Called to indicate that a press gesture has successfully completed.
     *
     * This should be used to fire a state changing event as if a button was pressed.
     */
    lateinit var onTap: (Offset) -> Unit

    /**
     * Whether or not to consume changes.
     */
    var consumeChanges: Boolean = true

    /**
     * True when we are primed to call [onTap] and may be consuming all down changes.
     */
    private var primed = false

    private var downPointers: MutableSet<PointerId> = mutableSetOf()
    private var upBlockedPointers: MutableSet<PointerId> = mutableSetOf()
    private var lastPxPosition: Offset? = null

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main) {

            if (primed &&
                changes.all { it.changedToUp() }
            ) {
                val pointerPxPosition: Offset = changes[0].previousPosition
                if (changes.fastAny { !upBlockedPointers.contains(it.id) }) {
                    // If we are primed, all pointers went up, and at least one of the pointers is
                    // not blocked, we can fire, reset, and consume all of the up events.
                    reset()
                    onTap.invoke(pointerPxPosition)
                    if (consumeChanges) {
                        changes.fastForEach {
                            it.consumeDownChange()
                        }
                    }
                    return
                } else {
                    lastPxPosition = pointerPxPosition
                }
            }

            if (changes.all { it.changedToDown() }) {
                // Reset in case we were incorrectly left waiting on a delayUp message.
                reset()
                // If all of the changes are down, can become primed.
                primed = true
            }

            if (primed) {
                changes.forEach {
                    if (it.changedToDown()) {
                        downPointers.add(it.id)
                    }
                    if (it.changedToUpIgnoreConsumed()) {
                        downPointers.remove(it.id)
                    }
                }
            }
        }

        if (pass == PointerEventPass.Final && primed) {

            val anyPositionChangeConsumed = changes.fastAny { it.positionChangeConsumed() }

            val noPointersInBounds =
                upBlockedPointers.isEmpty() && !changes.anyPointersInBounds(bounds)

            if (anyPositionChangeConsumed || noPointersInBounds) {
                // If we are on the final pass, we are primed, and either we aren't blocked and
                // all pointers are out of bounds.
                reset()
            }
        }
    }

    // TODO(shepshapard): This continues to be very confusing to use.  Have to come up with a better
//  way of easily expressing this.
    /**
     * Utility method that determines if any pointers are currently in [bounds].
     *
     * A pointer is considered in bounds if it is currently down and it's current
     * position is within the provided [bounds]
     *
     * @return True if at least one pointer is in bounds.
     */
    private fun List<PointerInputChange>.anyPointersInBounds(bounds: IntSize) =
        fastAny {
            it.pressed &&
                it.position.x >= 0 &&
                it.position.x < bounds.width &&
                it.position.y >= 0 &&
                it.position.y < bounds.height
        }

    override fun onCancel() {
        reset()
    }

    private fun reset() {
        primed = false
        upBlockedPointers.clear()
        downPointers.clear()
        lastPxPosition = null
    }
}

private data class TapPointerInputModifierImpl(
    override val pointerInputFilter: PointerInputFilter
) : PointerInputModifier