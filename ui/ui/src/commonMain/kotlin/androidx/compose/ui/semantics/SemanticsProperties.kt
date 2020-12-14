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
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility and testing.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension
 * instead of used directly.
 */
@VisibleForTesting
object SemanticsProperties {
    /**
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
     * @see SemanticsPropertyReceiver.stateDescription
     */
    val StateDescription = SemanticsPropertyKey<String>("StateDescription")

    /**
     * @see SemanticsPropertyReceiver.progressBarRangeInfo
     */
    val ProgressBarRangeInfo =
        SemanticsPropertyKey<ProgressBarRangeInfo>("ProgressBarRangeInfo")

    /**
     * The node is marked as heading for accessibility.
     *
     * @see SemanticsPropertyReceiver.heading
     */
    val Heading = SemanticsPropertyKey<Unit>("Heading")

    /**
     * @see SemanticsPropertyReceiver.disabled
     */
    val Disabled = SemanticsPropertyKey<Unit>("Disabled")

    /**
     * @see SemanticsPropertyReceiver.focused
     */
    val Focused = SemanticsPropertyKey<Boolean>("Focused")

    /**
     * @see SemanticsPropertyReceiver.invisibleToUser
     */
    @ExperimentalComposeUiApi
    val InvisibleToUser = SemanticsPropertyKey<Unit>(
        name = "InvisibleToUser",
        mergePolicy = { parentValue, _ ->
            parentValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.horizontalScrollAxisRange
     */
    val HorizontalScrollAxisRange =
        SemanticsPropertyKey<ScrollAxisRange>("HorizontalScrollAxisRange")

    /**
     * @see SemanticsPropertyReceiver.verticalScrollAxisRange
     */
    val VerticalScrollAxisRange =
        SemanticsPropertyKey<ScrollAxisRange>("VerticalScrollAxisRange")

    /**
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
     * @see SemanticsPropertyReceiver.dialog
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

    /**
     * The type of user interface element. Accessibility services might use this to describe the
     * element or do customizations. Most roles can be automatically resolved by the semantics
     * properties of this element. But some elements with subtle differences need an exact role. If
     * an exact role is not listed in [Role], this property should not be set and the framework will
     * automatically resolve it.
     *
     * @see SemanticsPropertyReceiver.role
     */
    val Role = SemanticsPropertyKey<Role>("Role")

    /**
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
     * @see SemanticsPropertyReceiver.textSelectionRange
     */
    val TextSelectionRange = SemanticsPropertyKey<TextRange>("TextSelectionRange")

    /**
     *  @see SemanticsPropertyReceiver.imeAction
     */
    val ImeAction = SemanticsPropertyKey<ImeAction>("ImeAction")

    /**
     * @see SemanticsPropertyReceiver.selected
     */
    val Selected = SemanticsPropertyKey<Boolean>("Selected")

    /**
     * @see SemanticsPropertyReceiver.toggleableState
     */
    val ToggleableState = SemanticsPropertyKey<ToggleableState>("ToggleableState")
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension
 * instead of used directly.
 */
@VisibleForTesting
object SemanticsActions {
    /**
     * @see SemanticsPropertyReceiver.getTextLayoutResult
     */
    val GetTextLayoutResult = SemanticsPropertyKey<AccessibilityAction<
            (MutableList<TextLayoutResult>) -> Boolean>>("GetTextLayoutResult")

    /**
     * @see SemanticsPropertyReceiver.onClick
     */
    val OnClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnClick")

    /**
     * @see SemanticsPropertyReceiver.onLongClick
     */
    val OnLongClick = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("OnLongClick")

    /**
     * @see SemanticsPropertyReceiver.scrollBy
     */
    val ScrollBy =
        SemanticsPropertyKey<AccessibilityAction<(x: Float, y: Float) -> Boolean>>("ScrollBy")

    /**
     * @see SemanticsPropertyReceiver.setProgress
     */
    val SetProgress =
        SemanticsPropertyKey<AccessibilityAction<(progress: Float) -> Boolean>>("SetProgress")

    /**
     * @see SemanticsPropertyReceiver.setSelection
     */
    val SetSelection = SemanticsPropertyKey<
        AccessibilityAction<(Int, Int, Boolean) -> Boolean>>("SetSelection")

    /**
     * @see SemanticsPropertyReceiver.setText
     */
    val SetText = SemanticsPropertyKey<
        AccessibilityAction<(AnnotatedString) -> Boolean>>("SetText")

    /**
     * @see SemanticsPropertyReceiver.copyText
     */
    val CopyText = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("CopyText")

    /**
     * @see SemanticsPropertyReceiver.cutText
     */
    val CutText = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("CutText")

    /**
     * @see SemanticsPropertyReceiver.pasteText
     */
    val PasteText = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("PasteText")

    /**
     * @see SemanticsPropertyReceiver.dismiss
     */
    val Dismiss = SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>("Dismiss")

    /**
     * @see SemanticsPropertyReceiver.customActions
     */
    val CustomActions =
        SemanticsPropertyKey<List<CustomAccessibilityAction>>("CustomActions")
}

/**
 * SemanticsPropertyKey is the infrastructure for setting key/value pairs inside semantics blocks
 * in a type-safe way.  Each key has one particular statically defined value type T.
 */
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
 * Data class for accessibility range information, to represent the status of a progress bar or
 * seekable progress bar.
 *
 * @param current current value in the range
 * @param range range of this node
 * @param steps if greater than 0, specifies the number of discrete values, evenly distributed
 * between across the whole value range. If 0, any value from the range specified can be chosen.
 */
data class ProgressBarRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>,
    @IntRange(from = 0) val steps: Int = 0
)

/**
 * The scroll state of one axis if this node is scrollable.
 *
 * @param value current scroll position value in pixels
 * @param maxValue maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
 * @param reverseScrolling for horizontal scroll, when this is `true`, 0 [value] will mean right,
 * when`false`, 0 [value] will mean left. For vertical scroll, when this is `true`, 0 [value] will
 * mean bottom, when `false`, 0 [value] will mean top
 */
data class ScrollAxisRange(
    val value: Float = 0f,
    val maxValue: Float = 0f,
    val reverseScrolling: Boolean = false
)

/**
 * The type of user interface element. Accessibility services might use this to describe the
 * element or do customizations. Most roles can be automatically resolved by the semantics
 * properties of this element. But some elements with subtle differences need an exact role. If an
 * exact role is not listed, [SemanticsPropertyReceiver.role] should not be set and the framework
 * will automatically resolve it.
 */
enum class Role {
    /**
     * This element is a button control. Associated semantics properties for accessibility:
     * [SemanticsProperties.Disabled], [SemanticsActions.OnClick]
     */
    Button,
    /**
     * This element is a Checkbox which is a component that represents two states (checked /
     * unchecked). Associated semantics properties for accessibility:
     * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
     * [SemanticsActions.OnClick]
     */
    Checkbox,
    /**
     * This element is a Switch which is a two state toggleable component that provides on/off
     * like options. Associated semantics properties for accessibility:
     * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
     * [SemanticsActions.OnClick]
     */
    Switch,
    /**
     * This element is a RadioButton which is a component to represent two states, selected and not
     * selected. Associated semantics properties for accessibility: [SemanticsProperties.Disabled],
     * [SemanticsProperties.StateDescription], [SemanticsActions.OnClick]
     */
    RadioButton,
    /**
     * This element is a Tab which represents a single page of content using a text label and/or
     * icon. A Tab also has two states: selected and not selected. Associated semantics properties
     * for accessibility: [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
     * [SemanticsActions.OnClick]
     */
    Tab
}

/**
 * SemanticsPropertyReceiver is the scope provided by semantics {} blocks, letting you set
 * key/value pairs primarily via extension functions.
 */
interface SemanticsPropertyReceiver {
    operator fun <T> set(key: SemanticsPropertyKey<T>, value: T)
}

/**
 * Developer-set content description of the semantics node.
 *
 * If this is not set, accessibility services will present the text of this node as the content
 * description.
 */
var SemanticsPropertyReceiver.contentDescription by SemanticsProperties.ContentDescription

/**
 * Developer-set state description of the semantics node.
 *
 * For example: on/off. If this not set, accessibility services will derive the state from
 * other semantics properties, like [ProgressBarRangeInfo], but it is not guaranteed and the format
 * will be decided by accessibility services.
 */
var SemanticsPropertyReceiver.stateDescription by SemanticsProperties.StateDescription

/**
 * The semantics is represents a range of possible values with a current value.
 * For example, when used on a slider control, this will allow screen readers to communicate
 * the slider's state.
 */
var SemanticsPropertyReceiver.progressBarRangeInfo by SemanticsProperties.ProgressBarRangeInfo

@Deprecated(
    "stateDescriptionRange was renamed to progressBarRangeInfo",
    ReplaceWith("progressBarRangeInfo", "androidx.compose.ui.semantics")
)
var SemanticsPropertyReceiver.stateDescriptionRange by SemanticsProperties.ProgressBarRangeInfo

/**
 * The node is marked as heading for accessibility.
 *
 * @see SemanticsProperties.Heading
 */
fun SemanticsPropertyReceiver.heading() {
    this[SemanticsProperties.Heading] = Unit
}

/**
 * Whether this semantics node is disabled. Note that proper [SemanticsActions] should still
 * be added when this property is set.
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
 * Whether this node is specially known to be invisible to the user.
 *
 * For example, if the node is currently occluded by a dark semitransparent
 * pane above it, then for all practical purposes the node is invisible to the user,
 * but the system cannot automatically determine that.  To make the screen reader linear
 * navigation skip over this type of invisible node, this property can be set.
 *
 * If looking for a way to hide semantics of small items from screenreaders because they're
 * redundant with semantics of their parent, consider
 * [SemanticsPropertyReceiver.replaceSemantics] instead.
 */
@ExperimentalComposeUiApi
fun SemanticsPropertyReceiver.invisibleToUser() {
    this[SemanticsProperties.InvisibleToUser] = Unit
}

@Deprecated(
    "hidden was renamed to invisibleToUser",
    ReplaceWith("invisibleToUser", "androidx.compose.ui.semantics")
)
@OptIn(ExperimentalComposeUiApi::class)
fun SemanticsPropertyReceiver.hidden() {
    this[SemanticsProperties.InvisibleToUser] = Unit
}

@Deprecated(
    "horizontalAccessibilityScrollState was renamed to horizontalScrollAxisRange",
    ReplaceWith("horizontalScrollAxisRange", "androidx.compose.ui.semantics")
)
var SemanticsPropertyReceiver.horizontalAccessibilityScrollState
by SemanticsProperties.HorizontalScrollAxisRange

@Deprecated(
    "verticalAccessibilityScrollState was renamed to verticalScrollAxisRange",
    ReplaceWith("verticalScrollAxisRange", "androidx.compose.ui.semantics")
)
var SemanticsPropertyReceiver.verticalAccessibilityScrollState
by SemanticsProperties.VerticalScrollAxisRange

/**
 * The horizontal scroll state of this node if this node is scrollable.
 */
var SemanticsPropertyReceiver.horizontalScrollAxisRange
by SemanticsProperties.HorizontalScrollAxisRange

/**
 * The vertical scroll state of this node if this node is scrollable.
 */
var SemanticsPropertyReceiver.verticalScrollAxisRange
by SemanticsProperties.VerticalScrollAxisRange

/**
 * Whether this semantics node represents a Popup. Not to be confused with if this node is
 * _part of_ a Popup.
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

/**
 * The type of user interface element. Accessibility services might use this to describe the
 * element or do customizations. Most roles can be automatically resolved by the semantics
 * properties of this element. But some elements with subtle differences need an exact role. If
 * an exact role is not listed in [Role], this property should not be set and the framework will
 * automatically resolve it.
 *
 * @see SemanticsProperties.Role
 */
var SemanticsPropertyReceiver.role by SemanticsProperties.Role

// TODO(b/138172781): Move to FoundationSemanticsProperties.kt
/**
 * Test tag attached to this semantics node.
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 */
var SemanticsPropertyReceiver.text by SemanticsProperties.Text

/**
 * Text selection range for edit text.
 */
var SemanticsPropertyReceiver.textSelectionRange by SemanticsProperties.TextSelectionRange

/**
 * Contains the IME action provided by the node.
 *
 * For example, "go to next form field" or "submit".
 */
var SemanticsPropertyReceiver.imeAction by SemanticsProperties.ImeAction

/**
 * Whether this element is selected (out of a list of possible selections).
 *
 * The presence of this property indicates that the element is selectable.
 */
var SemanticsPropertyReceiver.selected by SemanticsProperties.Selected

/**
 * The state of a toggleable component.
 *
 * The presence of this property indicates that the element is toggleable.
 */
var SemanticsPropertyReceiver.toggleableState
by SemanticsProperties.ToggleableState

/**
 * Custom actions which are defined by app developers.
 */
var SemanticsPropertyReceiver.customActions by SemanticsActions.CustomActions

/**
 * Action to get a Text/TextField node's [TextLayoutResult]. The result is the first element
 * of layout (the argument of the AccessibilityAction).
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
 * Action to be performed when the node is clicked (single-tapped).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is long clicked (long-pressed).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnLongClick] is called.
 */
fun SemanticsPropertyReceiver.onLongClick(label: String? = null, action: () -> Boolean) {
    this[SemanticsActions.OnLongClick] = AccessibilityAction(label, action)
}

/**
 * Action to scroll by a specified amount.
 *
 * Expected to be used in conjunction with verticalScrollAxisRange/horizontalScrollAxisRange.
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
 * Action to set the current value of the progress bar.
 *
 * Expected to be used in conjunction with progressBarRangeInfo.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(label: String? = null, action: (Float) -> Boolean) {
    this[SemanticsActions.SetProgress] = AccessibilityAction(label, action)
}

/**
 * Action to set the text contents of this node.
 *
 * Expected to be used on editable text fields.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetText] is called.
 */
fun SemanticsPropertyReceiver.setText(label: String? = null, action: (AnnotatedString) -> Boolean) {
    this[SemanticsActions.SetText] = AccessibilityAction(label, action)
}

/**
 * Action to set text selection by character index range.
 *
 * If this action is provided, the selection data must be provided
 * using [textSelectionRange].
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
 * Action to copy the text to the clipboard.
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
 * Action to cut the text and copy it to the clipboard.
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
 * Action to dismiss a dismissible node.
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