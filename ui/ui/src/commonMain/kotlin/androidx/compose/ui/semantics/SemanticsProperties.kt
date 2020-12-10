/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.annotation.IntRange
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility and testing.
 */
object SemanticsProperties {
    /**
     * Developer-set content description of the semantics node. If this is not set, accessibility
     * services will present the [Text] of this node as content part.
     *
     * @see SemanticsPropertyReceiver.contentDescription
     */
    val ContentDescription = SemanticsPropertyKey<String>(
        name = "ContentDescription",
        mergePolicy = { parentValue, childValue ->
            if (parentValue == null) {
                childValue
            } else {
                "$parentValue, $childValue"
            }
        }
    )

    /**
     * Developer-set state description of the semantics node. For example: on/off. If this not
     * set, accessibility services will derive the state from other semantics properties, like
     * [AccessibilityRangeInfo], but it is not guaranteed and the format will be decided by
     * accessibility services.
     *
     * @see SemanticsPropertyReceiver.stateDescription
     */
    val StateDescription = SemanticsPropertyKey<String>("StateDescription")

    /**
     * The node is a range with current value.
     *
     * @see SemanticsPropertyReceiver.stateDescriptionRange
     */
    val AccessibilityRangeInfo =
        SemanticsPropertyKey<AccessibilityRangeInfo>("AccessibilityRangeInfo")

    /**
     * Whether this semantics node is disabled.
     *
     * @see SemanticsPropertyReceiver.disabled
     */
    val Disabled = SemanticsPropertyKey<Unit>("Disabled")

    /**
     * Whether this semantics node is input focused.
     *
     * @see SemanticsPropertyReceiver.focused
     */
    val Focused = SemanticsPropertyKey<Boolean>("Focused")

    /**
     * Whether this semantics node is hidden. A hidden node is a node that is not visible for
     * accessibility. It will still be shown, but it will be skipped by accessibility services.
     *
     * @see SemanticsPropertyReceiver.hidden
     */
    val Hidden = SemanticsPropertyKey<Unit>(
        name = "Hidden",
        mergePolicy = { parentValue, _ ->
            parentValue
        }
    )

    /**
     * The horizontal scroll state of this node if this node is scrollable.
     *
     * @see SemanticsPropertyReceiver.horizontalAccessibilityScrollState
     */
    val HorizontalAccessibilityScrollState =
        SemanticsPropertyKey<AccessibilityScrollState>("HorizontalAccessibilityScrollState")

    /**
     * Whether this semantics node represents a Popup. Not to be confused with if this node is
     * _part of_ a Popup.
     *
     * @see SemanticsPropertyReceiver.popup
     */
    val IsPopup = SemanticsPropertyKey<Unit>(
        name = "IsPopup",
        mergePolicy = { _, _ ->
            throw IllegalStateException(
                "merge function called on unmergeable property IsPopup. " +
                    "A popup should not be a child of a clickable/focusable node."
            )
        }
    )

    /**
     * Whether this element is a Dialog. Not to be confused with if this element is _part of_ a
     * Dialog.
     */
    val IsDialog = SemanticsPropertyKey<Unit>(
        name = "IsDialog",
        mergePolicy = { _, _ ->
            throw IllegalStateException(
                "merge function called on unmergeable property IsDialog. " +
                    "A dialog should not be a child of a clickable/focusable node."
            )
        }
    )

    // TODO(b/138172781): Move to FoundationSemanticsProperties
    /**
     * Test tag attached to this semantics node.
     *
     * @see SemanticsPropertyReceiver.testTag
     */
    val TestTag = SemanticsPropertyKey<String>(
        name = "TestTag",
        mergePolicy = { parentValue, _ ->
            // Never merge TestTags, to avoid leaking internal test tags to parents.
            parentValue
        }
    )

    /**
     * Text of the semantics node. It must be the actual text displayed by this component instead
     * of developer-set content description.
     *
     * @see SemanticsPropertyReceiver.text
     */
    val Text = SemanticsPropertyKey<AnnotatedString>(
        name = "Text",
        mergePolicy = { parentValue, childValue ->
            if (parentValue == null) {
                childValue
            } else {
                buildAnnotatedString {
                    append(parentValue)
                    append(", ")
                    append(childValue)
                }
            }
        }
    )

    /**
     * Text selection range for edit text.
     *
     * @see TextRange
     * @see SemanticsPropertyReceiver.textSelectionRange
     */
    val TextSelectionRange = SemanticsPropertyKey<TextRange>("TextSelectionRange")

    /**
     * Contains the IME action provided by the node.
     *
     *  @see SemanticsPropertyReceiver.imeAction
     */
    val ImeAction = SemanticsPropertyKey<ImeAction>("ImeAction")

    /**
     * The vertical scroll state of this node if this node is scrollable.
     *
     * @see SemanticsPropertyReceiver.verticalAccessibilityScrollState
     */
    val VerticalAccessibilityScrollState =
        SemanticsPropertyKey<AccessibilityScrollState>("VerticalAccessibilityScrollState")

    /**
     * Whether this element is selected (out of a list of possible selections).
     * The presence of this property indicates that the element is selectable.
     *
     * @see SemanticsPropertyReceiver.selected
     */
    val Selected = SemanticsPropertyKey<Boolean>("Selected")

    /**
     * The state of a toggleable component.
     * The presence of this property indicates that the element is toggleable.
     *
     * @see SemanticsPropertyReceiver.toggleableState
     */
    val ToggleableState = SemanticsPropertyKey<ToggleableState>("ToggleableState")
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 */
object SemanticsActions {
    /**
     * Action to get a Text/TextField node's [TextLayoutResult]. The result is the first element
     * of layout(the argument of the AccessibilityAction).
     *
     * @see SemanticsPropertyReceiver.getTextLayoutResult
     */
    val GetTextLayoutResult = SemanticsPropertyKey<AccessibilityAction<
            (MutableList<TextLayoutResult>) -> Boolean>>("GetTextLayoutResult")

    /**
     * Action to be performed when the node is clicked.
     *
     * @see SemanticsPropertyReceiver.onClick
     */
    val OnClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnClick")

    /**
     * Action to be performed when the node is long clicked.
     *
     * @see SemanticsPropertyReceiver.onLongClick
     */
    val OnLongClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnLongClick")

    /**
     * Action to scroll to a specified position.
     *
     * @see SemanticsPropertyReceiver.scrollBy
     */
    val ScrollBy =
        SemanticsPropertyKey<AccessibilityAction<(x: Float, y: Float) -> Boolean>>("ScrollBy")

    /**
     * Action to set progress.
     *
     * @see SemanticsPropertyReceiver.setProgress
     */
    val SetProgress =
        SemanticsPropertyKey<AccessibilityAction<(progress: Float) -> Boolean>>("SetProgress")

    /**
     * Action to set selection. If this action is provided, the selection data must be provided
     * using [SemanticsProperties.TextSelectionRange].
     *
     * @see SemanticsPropertyReceiver.setSelection
     */
    val SetSelection = SemanticsPropertyKey<
        AccessibilityAction<(Int, Int, Boolean) -> Boolean>>("SetSelection")

    /**
     * Action to set the text of this node.
     *
     * @see SemanticsPropertyReceiver.setText
     */
    val SetText = SemanticsPropertyKey<
        AccessibilityAction<(AnnotatedString) -> Boolean>>("SetText")

    /**
     * Action to copy the text to the clipboard.
     *
     * @see SemanticsPropertyReceiver.copyText
     */
    val CopyText = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("CopyText")

    /**
     * Action to cut the text and copy it to the clipboard.
     *
     * @see SemanticsPropertyReceiver.cutText
     */
    val CutText = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("CutText")

    /**
     * Action to paste the text from the clipboard. Add it to indicate that element is open for
     * accepting paste data from the clipboard.
     * The element setting this property should also set the [SemanticsProperties.Focused] property.
     *
     * @see SemanticsPropertyReceiver.pasteText
     */
    val PasteText = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("PasteText")

    /**
     * Action to dismiss a dismissible node.
     *
     * @see SemanticsPropertyReceiver.dismiss
     */
    val Dismiss = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("Dismiss")

    /**
     * Custom actions which are defined by app developers.
     *
     * @see SemanticsPropertyReceiver.customActions
     */
    val CustomActions =
        SemanticsPropertyKey<List<CustomAccessibilityAction>>("CustomActions")
}

class SemanticsPropertyKey<T>(
    /**
     * The name of the property.  Should be the same as the constant from which it is accessed.
     */
    val name: String,
    internal val mergePolicy: (T?, T) -> T? = { parentValue, childValue ->
        parentValue ?: childValue
    }
) {
    /**
     * Method implementing the semantics merge policy of a particular key.
     *
     * When mergeDescendants is set on a semantics node, then this function will called for each
     * descendant node of a given key in depth-first-search order.  The parent
     * value accumulates the result of merging the values seen so far, similar to reduce().
     *
     * The default implementation returns the parent value if one exists, otherwise uses the
     * child element.  This means by default, a SemanticsNode with mergeDescendants = true
     * winds up with the first value found for each key in its subtree in depth-first-search order.
     */
    fun merge(parentValue: T?, childValue: T): T? {
        return mergePolicy(parentValue, childValue)
    }

    /**
     * Throws [UnsupportedOperationException].  Should not be called.
     */
    // TODO(KT-6519): Remove this getter
    // TODO(KT-32770): Cannot deprecate this either as the getter is considered called by "by"
    final operator fun getValue(thisRef: SemanticsPropertyReceiver, property: KProperty<*>): T {
        throw UnsupportedOperationException(
            "You cannot retrieve a semantics property directly - " +
                "use one of the SemanticsConfiguration.getOr* methods instead"
        )
    }

    final operator fun setValue(
        thisRef: SemanticsPropertyReceiver,
        property: KProperty<*>,
        value: T
    ) {
        thisRef[this] = value
    }

    override fun toString(): String {
        return "SemanticsPropertyKey: $name"
    }
}

/**
 * Data class for standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return
 * a boolean result indicating whether the action is successfully handled. For example, a scroll
 * forward action should return false if the widget is not enabled or has reached the end of the
 * list.
 */
data class AccessibilityAction<T : Function<Boolean>>(val label: CharSequence?, val action: T)

/**
 * Data class for custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 * arguments and return a boolean result indicating whether the action is successfully handled.
 */
data class CustomAccessibilityAction(val label: CharSequence, val action: () -> Boolean)

/**
 * Data class for accessibility range information.
 *
 * @param current current value in the range
 * @param range range of this node
 * @param steps if greater than 0, specifies the number of discrete values, evenly distributed
 * between across the whole value range. If 0, any value from the range specified can be chosen.
 */
data class AccessibilityRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>,
    @IntRange(from = 0) val steps: Int = 0
)

/**
 * The scroll state of this node if this node is scrollable.
 *
 * @param value current scroll position value in pixels
 * @param maxValue maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
 * @param reverseScrolling for horizontal scroll, when this is `true`, 0 [value] will mean right,
 * when`false`, 0 [value] will mean left. For vertical scroll, when this is `true`, 0 [value] will
 * mean bottom, when `false`, 0 [value] will mean top
 */
data class AccessibilityScrollState(
    val value: Float = 0f,
    val maxValue: Float = 0f,
    val reverseScrolling: Boolean = false
)

interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node. If this is not set, accessibility
 * services will present the text of this node as content part.
 *
 * @see SemanticsProperties.ContentDescription
 */
var SemanticsPropertyReceiver.contentDescription by SemanticsProperties.ContentDescription

@Deprecated(
    "accessibilityLabel was renamed to contentDescription",
    ReplaceWith("contentDescription", "androidx.compose.ui.semantics")
)
var SemanticsPropertyReceiver.accessibilityLabel by SemanticsProperties.ContentDescription

/**
 * Developer-set state description of the semantics node. For example: on/off. If this not
 * set, accessibility services will derive the state from other semantics properties, like
 * [AccessibilityRangeInfo], but it is not guaranteed and the format will be decided by
 * accessibility services.
 *
 * @see SemanticsProperties.StateDescription
 */
var SemanticsPropertyReceiver.stateDescription by SemanticsProperties.StateDescription

@Deprecated(
    "accessibilityValue was renamed to stateDescription",
    ReplaceWith("stateDescription", "androidx.compose.ui.semantics")
)
var SemanticsPropertyReceiver.accessibilityValue by SemanticsProperties.StateDescription

/**
 * The node is a range with current value.
 *
 * @see SemanticsProperties.AccessibilityRangeInfo
 */
var SemanticsPropertyReceiver.stateDescriptionRange by SemanticsProperties.AccessibilityRangeInfo

/**
 * Whether this semantics node is disabled.
 *
 * @see SemanticsProperties.Disabled
 */
fun SemanticsPropertyReceiver.disabled() {
    this[SemanticsProperties.Disabled] = Unit
}

/**
 * Whether this semantics node is focused.
 *
 * @See SemanticsProperties.Focused
 */
var SemanticsPropertyReceiver.focused by SemanticsProperties.Focused

/**
 * Whether this semantics node is hidden. A hidden node is a node that is not visible for
 * accessibility.
 *
 * @See SemanticsProperties.Hidden
 */
fun SemanticsPropertyReceiver.hidden() {
    this[SemanticsProperties.Hidden] = Unit
}

/**
 * The horizontal scroll state of this node if this node is scrollable.
 *
 * @see SemanticsProperties.HorizontalAccessibilityScrollState
 */
var SemanticsPropertyReceiver.horizontalAccessibilityScrollState
by SemanticsProperties.HorizontalAccessibilityScrollState

/**
 * The vertical scroll state of this node if this node is scrollable.
 *
 * @see SemanticsProperties.VerticalAccessibilityScrollState
 */
var SemanticsPropertyReceiver.verticalAccessibilityScrollState
by SemanticsProperties.VerticalAccessibilityScrollState

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is
 * _part of_ a Popup.
 *
 * @See SemanticsProperties.IsPopup
 */
fun SemanticsPropertyReceiver.popup() {
    this[SemanticsProperties.IsPopup] = Unit
}

/**
 * Whether this element is a Dialog. Not to be confused with if this element is _part of_ a Dialog.
 */
fun SemanticsPropertyReceiver.dialog() {
    this[SemanticsProperties.IsDialog] = Unit
}

// TODO(b/138172781): Move to FoundationSemanticsProperties.kt
/**
 * Test tag attached to this semantics node.
 *
 * @see SemanticsPropertyReceiver.testTag
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 *
 * @see SemanticsProperties.Text
 */
var SemanticsPropertyReceiver.text by SemanticsProperties.Text

/**
 * Text selection range for edit text.
 *
 * @see TextRange
 * @see SemanticsProperties.TextSelectionRange
 */
var SemanticsPropertyReceiver.textSelectionRange by SemanticsProperties.TextSelectionRange

/**
 * Contains the IME action provided by the node.
 *
 *  @see SemanticsProperties.ImeAction
 */
var SemanticsPropertyReceiver.imeAction by SemanticsProperties.ImeAction

/**
 * Whether this element is selected (out of a list of possible selections).
 * The presence of this property indicates that the element is selectable.
 *
 * @see SemanticsProperties.Selected
 */
var SemanticsPropertyReceiver.selected by SemanticsProperties.Selected

/**
 * The state of a toggleable component.
 * The presence of this property indicates that the element is toggleable.
 *
 * @see SemanticsProperties.ToggleableState
 */
var SemanticsPropertyReceiver.toggleableState
by SemanticsProperties.ToggleableState

/**
 * Custom actions which are defined by app developers.
 *
 * @see SemanticsPropertyReceiver.customActions
 */
var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/**
 * This function adds the [SemanticsActions.GetTextLayoutResult] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.GetTextLayoutResult] is called.
 */
fun SemanticsPropertyReceiver.getTextLayoutResult(
    label: String? = null,
    action: (MutableList<TextLayoutResult>) -> Boolean
) {
    this[SemanticsActions.GetTextLayoutResult] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.OnClick] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.OnLongClick] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnLongClick] is called.
 */
fun SemanticsPropertyReceiver.onLongClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnLongClick] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.ScrollBy] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.ScrollBy] is called.
 */
fun SemanticsPropertyReceiver.scrollBy(
    label: String? = null,
    action: (x: Float, y: Float) -> Boolean
) {
    this[SemanticsActions.ScrollBy] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.SetProgress] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(label: String? = null, action: (Float) -> Boolean) {
    this[SemanticsActions.SetProgress] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.SetText] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetText] is called.
 */
fun SemanticsPropertyReceiver.setText(label: String? = null, action: (AnnotatedString) -> Boolean) {
    this[SemanticsActions.SetText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.SetSelection] to the [SemanticsPropertyReceiver]. If
 * this action is provided, the selection data must be provided using
 * [SemanticsProperties.TextSelectionRange].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetSelection] is called.
 */
fun SemanticsPropertyReceiver.setSelection(
    label: String? = null,
    action: (
        startIndex: Int,
        endIndex: Int,
        traversalMode: Boolean
    ) -> Boolean
) {
    this[SemanticsActions.SetSelection] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.CopyText] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CopyText] is called.
 */
fun SemanticsPropertyReceiver.copyText(
    label: String? = null,
    action: () -> Boolean
) {
    this[SemanticsActions.CopyText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.CutText] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.CutText] is called.
 */
fun SemanticsPropertyReceiver.cutText(
    label: String? = null,
    action: () -> Boolean
) {
    this[SemanticsActions.CutText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.PasteText] to the [SemanticsPropertyReceiver].
 * Use it to indicate that element is open for accepting paste data from the clipboard. There is
 * no need to check if the clipboard data available as this is done by the framework.
 * For this action to be triggered, the element must also have the [SemanticsProperties.Focused]
 * property set.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.PasteText] is called.
 *
 * @see focused
 */
fun SemanticsPropertyReceiver.pasteText(
    label: String? = null,
    action: () -> Boolean
) {
    this[SemanticsActions.PasteText] = AccessibilityAction(label, action)
}

/**
 * This function adds the [SemanticsActions.Dismiss] to the [SemanticsPropertyReceiver].
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Dismiss] is called.
 */
fun SemanticsPropertyReceiver.dismiss(
    label: String? = null,
    action: () -> Boolean
) {
    this[SemanticsActions.Dismiss] = AccessibilityAction(label, action)
}