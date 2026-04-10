/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.components.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Standard Material delay before showing a ripple to avoid flashing during scrolls
private const val TapIndicationDelay = 40L

/**
 * Helper to extract the hardware tool type (Touch, Mouse, etc.) from a [PointerEvent].
 */
val PointerEvent.pointerType: PointerType
    get() = changes.firstOrNull()?.type ?: PointerType.Unknown

/**
 * Evaluates to `true` if this [PointerEvent] represents a primary interaction.
 */
val PointerEvent.isPrimaryAction: Boolean
    get() = isPrimaryPointerAction(pointerType, buttons)

/**
 * Evaluates to `true` if this [PointerEvent] represents a secondary interaction.
 */
val PointerEvent.isSecondaryAction: Boolean
    get() = isSecondaryPointerAction(buttons)

/**
 * Evaluates to `true` if this [PointerEvent] represents a middle-click interaction.
 */
val PointerEvent.isTertiaryAction: Boolean
    get() = isTertiaryPointerAction(buttons)


/**
 * Represents a pointer click event, encapsulating hardware-specific metadata.
 *
 * This payload provides rich context about the interaction, enabling advanced multiplatform
 * behaviors such as secondary clicks (context menus) or keyboard-modified clicks (shift-click selection).
 *
 * @property position The coordinate of the click, relative to the bounds of the component.
 * @property buttons The state of the pointer buttons (e.g., Primary/Left, Secondary/Right) active during the click.
 * This value is `null` if the click was synthesized via software (such as a keyboard action or accessibility service).
 * @property type The type of pointer device that triggered the click.
 * @property keyboardModifiers The state of the keyboard modifiers (e.g., Shift, Ctrl, Alt) active during the click.
 */
@ExperimentalFoundationApi
class PointerClickEvent(
    val position: Offset,
    val buttons: PointerButtons?,
    val type: PointerType,
    val keyboardModifiers: PointerKeyboardModifiers
) {
    /**
     * Evaluates to `true` if this event represents a primary interaction.
     * This includes physical primary clicks (Touch, Mouse-Left, Stylus-Tip)
     * as well as synthesized semantic clicks (Keyboard Enter, Accessibility).
     */
    val isPrimaryAction: Boolean
        get() = isPrimaryPointerAction(type, buttons)

    /**
     * Evaluates to `true` if this event represents a physical secondary click.
     * This supports both Right-Mouse clicks and Stylus Barrel-Button clicks.
     */
    val isSecondaryAction: Boolean
        get() = isSecondaryPointerAction(buttons)

    /**
     * Evaluates to `true` if this event represents a physical eraser interaction
     * (the back-end of a supported stylus).
     */
    val isEraser: Boolean
        get() = isEraserPointerAction(type)

    /**
     * Evaluates to `true` if this event represents a physical middle-mouse click.
     */
    val isTertiaryAction: Boolean
        get() = isTertiaryPointerAction(buttons)
}


/**
 * Configures a component to receive primary pointer click events using a simple no-argument callback.
 *
 * This is a convenience overload for the common case where only primary interactions (touch,
 * left-mouse, stylus-tip, or accessibility) are needed and no pointer metadata is required.
 * It is named distinctly from [onPointerClick] to avoid Kotlin overload-resolution ambiguity
 * between `() -> Unit` and `(PointerClickEvent) -> Unit` lambda types.
 *
 * For secondary/tertiary clicks, modifier-key chords, or access to raw [PointerButtons],
 * use [onPointerClick] with a `(PointerClickEvent) -> Unit` callback instead.
 *
 * @param enabled Controls the enabled state of the component. When `false`, [onClick] will not be invoked,
 * and the element will be exposed as disabled to accessibility services.
 * @param onClickLabel Semantic label for the click action, used by accessibility services.
 * @param role The semantic purpose of the user interface element (e.g., [Role.Button]).
 * @param interactionSource The [MutableInteractionSource] used to dispatch [PressInteraction]s.
 * If `null`, a default source is created and remembered internally.
 * @param onClick Invoked when a successful primary click is recognized.
 */
@ExperimentalFoundationApi
fun Modifier.onPrimaryClick(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = this.onPointerClick(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    interactionSource = interactionSource,
    triggerPressInteraction = { it.isPrimaryAction },
    onClick = { event ->
        if (event.isPrimaryAction) {
            onClick()
        }
    }
)

/**
 * Configures a component to receive pointer click events alongside hardware-specific metadata.
 *
 * Exposes raw [PointerButtons] and [PointerKeyboardModifiers]
 * to the callback. This is essential for supporting complex, multiplatform interactions, such as alternative
 * button clicks or modifier-key chords.
 *
 * By default, this modifier follows Material Design guidelines: it responds to all pointer interactions
 * but only triggers visual ripple effects on primary actions (e.g., touch or left-click). This behavior
 * can be customized via the [triggerPressInteraction] parameter.
 *
 * @param enabled Controls the enabled state of the component. When `false`, [onClick] will not be invoked,
 * and the element will be exposed as disabled to accessibility services.
 * @param onClickLabel Semantic label for the click action, used by accessibility services.
 * @param role The semantic purpose of the user interface element (e.g., [Role.Button]).
 * @param interactionSource The [MutableInteractionSource] used to dispatch [PressInteraction]s.
 * If `null`, a default source is created and remembered internally.
 * @param triggerPressInteraction A predicate evaluating a raw [PointerEvent] to determine if the interaction
 * should transition the component into a pressed state (triggering visual indications). By default, this is
 * restricted to primary clicks.
 * @param onClick Invoked when a successful click is recognized, providing the [PointerClickEvent] metadata.
 */
@ExperimentalFoundationApi
fun Modifier.onPointerClick(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    triggerPressInteraction: (PointerEvent) -> Boolean = { it.isPrimaryAction },
    onClick: (PointerClickEvent) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "onPointerClick"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indication"] = "LocalIndication.current"
        properties["triggerPressInteraction"] = triggerPressInteraction
        properties["onClick"] = onClick
    }
) {
    val defaultIndication = LocalIndication.current
    onPointerClick(
        interactionSource = interactionSource,
        indication = defaultIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        triggerPressInteraction = triggerPressInteraction,
        onClick = onClick
    )
}

/**
 * Configures a component to receive pointer click events with precise control over visual indications.
 *
 * Use this overload when you need strict control over the [Indication] behavior:
 * - Pass a custom [Indication] (such as `LocalIndication.current`) to enable visual feedback.
 * - Pass `null` to disable press effects entirely.
 *
 * @param interactionSource The [MutableInteractionSource] used to dispatch [PressInteraction] events.
 * If `null`, a default source is created and remembered internally.
 * @param indication The visual effect applied when the element transitions to a pressed state.
 * Pass `null` to disable standard visual indication.
 * @param enabled Controls the enabled state of the component. When `false`, input is ignored,
 * and the element is exposed as disabled to accessibility services.
 * @param onClickLabel Semantic label for the click action, used by accessibility services.
 * @param role The semantic purpose of the user interface element (e.g., [Role.Button]).
 * @param triggerPressInteraction A predicate evaluating a raw [PointerEvent] to determine if the down event
 * should trigger a pressed state.
 * @param onClick Invoked when a successful click is recognized, providing the [PointerClickEvent] metadata.
 */
@ExperimentalFoundationApi
fun Modifier.onPointerClick(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    triggerPressInteraction: (PointerEvent) -> Boolean = { it.isPrimaryAction },
    onClick: (PointerClickEvent) -> Unit
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "onPointerClick"
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indication"] = indication
        properties["triggerPressInteraction"] = triggerPressInteraction
        properties["onClick"] = onClick
    }
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    this
        .focusable(enabled = enabled, interactionSource = resolvedInteractionSource)
        .hoverable(enabled = enabled, interactionSource = resolvedInteractionSource)
        .then(
            PointerClickElement(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                interactionSource = resolvedInteractionSource,
                triggerPressInteraction = triggerPressInteraction,
                onClick = onClick
            )
        )
        .indication(
            interactionSource = resolvedInteractionSource,
            indication = indication
        )
}

@OptIn(ExperimentalFoundationApi::class)
private class PointerClickElement(
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role?,
    private val interactionSource: MutableInteractionSource,
    private val triggerPressInteraction: (PointerEvent) -> Boolean,
    private val onClick: (PointerClickEvent) -> Unit
) : ModifierNodeElement<PointerClickNode>() {

    override fun create() = PointerClickNode(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        triggerPressInteraction = triggerPressInteraction,
        onClick = onClick
    )

    override fun update(node: PointerClickNode) {
        node.update(
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            interactionSource = interactionSource,
            triggerPressInteraction = triggerPressInteraction,
            onClick = onClick
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointerClickElement) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (interactionSource != other.interactionSource) return false
        // Lambda identity comparison is intentional: Compose does not guarantee
        // structural equality for lambdas, and using !== here correctly forces
        // update() on every recomposition that provides a new lambda capture.
        if (triggerPressInteraction !== other.triggerPressInteraction) return false
        if (onClick !== other.onClick) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + triggerPressInteraction.hashCode()
        result = 31 * result + onClick.hashCode()
        return result
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class PointerClickNode(
    private var enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    private var interactionSource: MutableInteractionSource,
    private var triggerPressInteraction: (PointerEvent) -> Boolean,
    private var onClick: (PointerClickEvent) -> Unit
) : DelegatingNode(),
    PointerInputModifierNode,
    SemanticsModifierNode,
    LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private var trackedPointerId: PointerId? = null
    private var downEvent: PointerInputChange? = null
    private var downButtons: PointerButtons? = null
    private var downKeyboardModifiers: PointerKeyboardModifiers? = null

    private var pressInteraction: PressInteraction.Press? = null
    private var delayJob: Job? = null

    private var componentSize: IntSize = IntSize.Zero
    private var centerOffset: Offset = Offset.Zero
    private var touchPadding: Size = Size.Zero

    override fun onRemeasured(size: IntSize) {
        componentSize = size
        centerOffset = size.center.toOffset()

        // Cache touch padding calculation to avoid allocations on the hot MOVE path
        val minimumTouchTargetSizeDp = currentValueOf(LocalViewConfiguration).minimumTouchTargetSize
        val minimumTouchTargetSize = with(requireDensity()) { minimumTouchTargetSizeDp.toSize() }
        val horizontal = max(0f, minimumTouchTargetSize.width - size.width) / 2f
        val vertical = max(0f, minimumTouchTargetSize.height - size.height) / 2f
        touchPadding = Size(horizontal, vertical)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Main) {
            if (trackedPointerId == null) {
                val downChange = pointerEvent.changes.firstOrNull { it.changedToDown() && !it.isConsumed }
                if (downChange != null) {
                    handleDownEvent(downChange, pointerEvent)
                }
            } else {
                // Lock onto the specific pointer we are tracking
                val trackedPointer = pointerEvent.changes.firstOrNull { it.id == trackedPointerId }
                if (trackedPointer == null) {
                    cancelInput()
                    return
                }

                if (trackedPointer.changedToUp()) {
                    handleUpEvent(trackedPointer, pointerEvent)
                } else {
                    handleNonUpEventIfNeeded(trackedPointer)
                }
            }
        } else if (pass == PointerEventPass.Final) {
            checkForCancellation(pointerEvent)
        }
    }

    private fun handleDownEvent(down: PointerInputChange, pointerEvent: PointerEvent) {
        // BUG FIX: Only consume the down event (and begin tracking state) when the
        // component is enabled. Previously, consume() was called unconditionally,
        // which prevented parent scroll / drag handlers from receiving the event
        // even when this component was fully disabled.
        if (!enabled) return

        down.consume()
        this.downEvent = down
        this.trackedPointerId = down.id
        this.downButtons = pointerEvent.buttons
        this.downKeyboardModifiers = pointerEvent.keyboardModifiers

        if (triggerPressInteraction(pointerEvent)) {
            val isTouch = down.type == PointerType.Touch || down.type == PointerType.Unknown
            handlePressInteractionStart(down.position, isTouch)
        }
    }

    private fun handleUpEvent(upChange: PointerInputChange, pointerEvent: PointerEvent) {
        upChange.consume()

        // Invariant: If trackedPointerId is not null (which got us here), downEvent must exist.
        val down = requireNotNull(downEvent) { "Up event received without prior down event" }

        // Note: Deliberately skipping a final bounds check on the UP frame to match
        // upstream Modifier.clickable behavior. If a fast drag-release results in a UP
        // frame slightly out of bounds, it is still registered as a successful click.
        if (enabled) {
            handlePressInteractionRelease(down.position)
            val event = PointerClickEvent(
                position = upChange.position,
                buttons = downButtons,
                type = upChange.type,
                keyboardModifiers = downKeyboardModifiers ?: pointerEvent.keyboardModifiers
            )
            onClick(event)
        }

        // Interaction is already released; only reset the hardware tracking state.
        resetPointerState()
    }

    private fun handleNonUpEventIfNeeded(trackedPointer: PointerInputChange) {
        if (trackedPointer.isConsumed || trackedPointer.isOutOfBounds(componentSize, touchPadding)) {
            cancelInput()
        }
    }

    private fun checkForCancellation(pointerEvent: PointerEvent) {
        if (trackedPointerId != null) {
            val trackedPointer = pointerEvent.changes.firstOrNull { it.id == trackedPointerId }
            // If the event was consumed by a parent (like a Scrollable) on the Main pass,
            // we will see it here as consumed on the Final pass.
            if (trackedPointer != null && trackedPointer.isConsumed && trackedPointer !== downEvent) {
                cancelInput()
            }
        }
    }

    override fun onCancelPointerInput() {
        cancelInput()
    }

    private fun resetPointerState() {
        trackedPointerId = null
        downEvent = null
        downButtons = null
        downKeyboardModifiers = null
    }

    private fun cancelInput() {
        resetPointerState()
        handlePressInteractionCancel()
    }

    private fun handlePressInteractionStart(offset: Offset, isTouch: Boolean) {
        val press = PressInteraction.Press(offset)

        if (isTouch) {
            delayJob = coroutineScope.launch {
                delay(TapIndicationDelay)
                interactionSource.emit(press)
                pressInteraction = press
            }
        } else {
            pressInteraction = press
            coroutineScope.launch { interactionSource.emit(press) }
        }
    }

    private fun handlePressInteractionRelease(offset: Offset) {
        val job = delayJob
        if (job?.isActive == true) {
            job.cancel()
            coroutineScope.launch {
                job.join()
                val press = PressInteraction.Press(offset)
                val release = PressInteraction.Release(press)
                interactionSource.emit(press)
                interactionSource.emit(release)
            }
        } else {
            pressInteraction?.let {
                coroutineScope.launch { interactionSource.emit(PressInteraction.Release(it)) }
            }
        }
        pressInteraction = null
        delayJob = null
    }

    private fun handlePressInteractionCancel() {
        val job = delayJob
        if (job?.isActive == true) {
            job.cancel()
        } else {
            pressInteraction?.let {
                coroutineScope.launch { interactionSource.emit(PressInteraction.Cancel(it)) }
            }
        }
        pressInteraction = null
        delayJob = null
    }

    fun update(
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        interactionSource: MutableInteractionSource,
        triggerPressInteraction: (PointerEvent) -> Boolean,
        onClick: (PointerClickEvent) -> Unit
    ) {
        if (this.enabled != enabled) {
            if (!enabled) cancelInput()
            this.enabled = enabled
            invalidateSemantics()
        }
        if (this.onClickLabel != onClickLabel || this.role != role) {
            this.onClickLabel = onClickLabel
            this.role = role
            invalidateSemantics()
        }
        if (this.interactionSource != interactionSource) {
            cancelInput()
            this.interactionSource = interactionSource
        }
        this.triggerPressInteraction = triggerPressInteraction
        this.onClick = onClick
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (this@PointerClickNode.role != null) {
            role = this@PointerClickNode.role!!
        }
        onClick(
            action = {
                val currentModifiers = try {
                    currentValueOf(LocalWindowInfo).keyboardModifiers
                } catch (_: Exception) {
                    PointerEvent(emptyList()).keyboardModifiers
                }
                val synthesizedEvent = PointerClickEvent(
                    position = centerOffset,
                    buttons = null,
                    type = PointerType.Unknown,
                    keyboardModifiers = currentModifiers
                )
                onClick(synthesizedEvent)
                true
            },
            label = onClickLabel
        )
        if (!enabled) {
            disabled()
        }
    }
}


/**
 * Core logic for determining a primary action.
 */
private fun isPrimaryPointerAction(type: PointerType, buttons: PointerButtons?): Boolean {
    return buttons == null ||
            type == PointerType.Touch ||
            type == PointerType.Unknown ||
            (type == PointerType.Stylus && !buttons.isPrimaryPressed
                    && !buttons.isSecondaryPressed && !buttons.isTertiaryPressed) ||
            buttons.isPrimaryPressed
}

/**
 * Core logic for determining a secondary action.
 */
private fun isSecondaryPointerAction(buttons: PointerButtons?): Boolean {
    return buttons?.isSecondaryPressed == true
}

/**
 * Core logic for determining a tertiary action.
 */
private fun isTertiaryPointerAction(buttons: PointerButtons?): Boolean {
    return buttons?.isTertiaryPressed == true
}

/**
 * Core logic for determining an eraser action.
 */
private fun isEraserPointerAction(type: PointerType): Boolean {
    return type == PointerType.Eraser
}