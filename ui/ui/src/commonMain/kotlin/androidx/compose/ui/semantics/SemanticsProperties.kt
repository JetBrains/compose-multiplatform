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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import kotlin.reflect.KProperty

/**
 * General semantics properties, mainly used for accessibility and testing.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension
 * instead of used directly.
 */
/*@VisibleForTesting*/
object SemanticsProperties {
    /**
     * @see SemanticsPropertyReceiver.contentDescription
     */
    val ContentDescription = SemanticsPropertyKey<List<String>>(
        name = "ContentDescription",
        mergePolicy = { parentValue, childValue ->
            parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
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
     * @see SemanticsPropertyReceiver.paneTitle
     */
    val PaneTitle = SemanticsPropertyKey<String>(
        name = "PaneTitle",
        mergePolicy = { _, _ ->
            throw IllegalStateException(
                "merge function called on unmergeable property PaneTitle."
            )
        }
    )

    /** @see SemanticsPropertyReceiver.selectableGroup */
    val SelectableGroup = SemanticsPropertyKey<Unit>("SelectableGroup")

    /** @see SemanticsPropertyReceiver.collectionInfo */
    val CollectionInfo = SemanticsPropertyKey<CollectionInfo>("CollectionInfo")

    /** @see SemanticsPropertyReceiver.collectionItemInfo */
    val CollectionItemInfo = SemanticsPropertyKey<CollectionItemInfo>("CollectionItemInfo")

    /**
     * @see SemanticsPropertyReceiver.heading
     */
    val Heading = SemanticsPropertyKey<Unit>("Heading")

    /**
     * @see SemanticsPropertyReceiver.disabled
     */
    val Disabled = SemanticsPropertyKey<Unit>("Disabled")

    /**
     * @see SemanticsPropertyReceiver.liveRegion
     */
    val LiveRegion = SemanticsPropertyKey<LiveRegionMode>("LiveRegion")

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
    val Role = SemanticsPropertyKey<Role>("Role") { parentValue, _ -> parentValue }

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
    val Text = SemanticsPropertyKey<List<AnnotatedString>>(
        name = "Text",
        mergePolicy = { parentValue, childValue ->
            parentValue?.toMutableList()?.also { it.addAll(childValue) } ?: childValue
        }
    )

    /**
     * @see SemanticsPropertyReceiver.editableText
     */
    val EditableText = SemanticsPropertyKey<AnnotatedString>(name = "EditableText")

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

    /**
     * @see SemanticsPropertyReceiver.password
     */
    val Password = SemanticsPropertyKey<Unit>("Password")

    /**
     * @see SemanticsPropertyReceiver.error
     */
    val Error = SemanticsPropertyKey<String>("Error")

    /**
     * @see SemanticsPropertyReceiver.indexForKey
     */
    val IndexForKey = SemanticsPropertyKey<(Any) -> Int>("IndexForKey")
}

/**
 * Ths object defines keys of the actions which can be set in semantics and performed on the
 * semantics node.
 *
 * Each of these is intended to be set by the respective SemanticsPropertyReceiver extension
 * instead of used directly.
 */
/*@VisibleForTesting*/
object SemanticsActions {
    /**
     * @see SemanticsPropertyReceiver.getTextLayoutResult
     */
    val GetTextLayoutResult =
        ActionPropertyKey<(MutableList<TextLayoutResult>) -> Boolean>("GetTextLayoutResult")

    /**
     * @see SemanticsPropertyReceiver.onClick
     */
    val OnClick = ActionPropertyKey<() -> Boolean>("OnClick")

    /**
     * @see SemanticsPropertyReceiver.onLongClick
     */
    val OnLongClick = ActionPropertyKey<() -> Boolean>("OnLongClick")

    /**
     * @see SemanticsPropertyReceiver.scrollBy
     */
    val ScrollBy = ActionPropertyKey<(x: Float, y: Float) -> Boolean>("ScrollBy")

    /**
     * @see SemanticsPropertyReceiver.scrollToIndex
     */
    val ScrollToIndex = ActionPropertyKey<(Int) -> Boolean>("ScrollToIndex")

    /**
     * @see SemanticsPropertyReceiver.setProgress
     */
    val SetProgress = ActionPropertyKey<(progress: Float) -> Boolean>("SetProgress")

    /**
     * @see SemanticsPropertyReceiver.setSelection
     */
    val SetSelection = ActionPropertyKey<(Int, Int, Boolean) -> Boolean>("SetSelection")

    /**
     * @see SemanticsPropertyReceiver.setText
     */
    val SetText = ActionPropertyKey<(AnnotatedString) -> Boolean>("SetText")

    /**
     * @see SemanticsPropertyReceiver.copyText
     */
    val CopyText = ActionPropertyKey<() -> Boolean>("CopyText")

    /**
     * @see SemanticsPropertyReceiver.cutText
     */
    val CutText = ActionPropertyKey<() -> Boolean>("CutText")

    /**
     * @see SemanticsPropertyReceiver.pasteText
     */
    val PasteText = ActionPropertyKey<() -> Boolean>("PasteText")

    /**
     * @see SemanticsPropertyReceiver.expand
     */
    val Expand = ActionPropertyKey<() -> Boolean>("Expand")

    /**
     * @see SemanticsPropertyReceiver.collapse
     */
    val Collapse = ActionPropertyKey<() -> Boolean>("Collapse")

    /**
     * @see SemanticsPropertyReceiver.dismiss
     */
    val Dismiss = ActionPropertyKey<() -> Boolean>("Dismiss")

    /**
     * @see SemanticsPropertyReceiver.requestFocus
     */
    val RequestFocus = ActionPropertyKey<() -> Boolean>("RequestFocus")

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
        return throwSemanticsGetNotSupported()
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

private fun <T> throwSemanticsGetNotSupported(): T {
    throw UnsupportedOperationException(
        "You cannot retrieve a semantics property directly - " +
            "use one of the SemanticsConfiguration.getOr* methods instead"
    )
}

/**
 * Standard accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should return
 * a boolean result indicating whether the action is successfully handled. For example, a scroll
 * forward action should return false if the widget is not enabled or has reached the end of the
 * list. If multiple semantics blocks with the same AccessibilityAction are provided, the
 * resulting AccessibilityAction's label/action will be the label/action of the outermost
 * modifier with this key and nonnull label/action, or null if no nonnull label/action is found.
 */
class AccessibilityAction<T : Function<Boolean>>(val label: String?, val action: T?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessibilityAction<*>) return false

        if (label != other.label) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        result = 31 * result + action.hashCode()
        return result
    }

    override fun toString(): String {
        return "AccessibilityAction(label=$label, action=$action)"
    }
}

@Suppress("NOTHING_TO_INLINE")
// inline to break static initialization cycle issue
private inline fun <T : Function<Boolean>> ActionPropertyKey(
    name: String
): SemanticsPropertyKey<AccessibilityAction<T>> {
    return SemanticsPropertyKey(
        name = name,
        mergePolicy = { parentValue, childValue ->
            AccessibilityAction(
                parentValue?.label ?: childValue.label,
                parentValue?.action ?: childValue.action
            )
        }
    )
}

/**
 * Custom accessibility action.
 *
 * @param label The description of this action
 * @param action The function to invoke when this action is performed. The function should have no
 * arguments and return a boolean result indicating whether the action is successfully handled.
 */
class CustomAccessibilityAction(val label: String, val action: () -> Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomAccessibilityAction) return false

        if (label != other.label) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + action.hashCode()
        return result
    }

    override fun toString(): String {
        return "CustomAccessibilityAction(label=$label, action=$action)"
    }
}

/**
 * Accessibility range information, to represent the status of a progress bar or
 * seekable progress bar.
 *
 * @param current current value in the range
 * @param range range of this node
 * @param steps if greater than `0`, specifies the number of discrete values, evenly distributed
 * between across the whole value range. If `0`, any value from the range specified can be chosen.
 * Cannot be less than `0`.
 */
class ProgressBarRangeInfo(
    val current: Float,
    val range: ClosedFloatingPointRange<Float>,
    /*@IntRange(from = 0)*/
    val steps: Int = 0
) {
    companion object {
        /**
         * Accessibility range information to present indeterminate progress bar
         */
        val Indeterminate = ProgressBarRangeInfo(0f, 0f..0f)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgressBarRangeInfo) return false

        if (current != other.current) return false
        if (range != other.range) return false
        if (steps != other.steps) return false

        return true
    }

    override fun hashCode(): Int {
        var result = current.hashCode()
        result = 31 * result + range.hashCode()
        result = 31 * result + steps
        return result
    }

    override fun toString(): String {
        return "ProgressBarRangeInfo(current=$current, range=$range, steps=$steps)"
    }
}

/**
 * Information about the collection.
 *
 * A collection of items has [rowCount] rows and [columnCount] columns.
 * For example, a vertical list is a collection with one column, as many rows as the list items
 * that are important for accessibility; A table is a collection with several rows and several
 * columns.
 *
 * @param rowCount the number of rows in the collection, or -1 if unknown
 * @param columnCount the number of columns in the collection, or -1 if unknown
 */
class CollectionInfo(val rowCount: Int, val columnCount: Int)

/**
 * Information about the item of a collection.
 *
 * A collection item is contained in a collection, it starts at a given [rowIndex] and
 * [columnIndex] in the collection, and spans one or more rows and columns. For example, a header
 * of two related table columns starts at the first row and the first column, spans one row and
 * two columns.
 *
 * @param rowIndex the index of the row at which item is located
 * @param rowSpan the number of rows the item spans
 * @param columnIndex the index of the column at which item is located
 * @param columnSpan the number of columns the item spans
 */
class CollectionItemInfo(
    val rowIndex: Int,
    val rowSpan: Int,
    val columnIndex: Int,
    val columnSpan: Int
)

/**
 * The scroll state of one axis if this node is scrollable.
 *
 * @param value current 0-based scroll position value (either in pixels, or lazy-item count)
 * @param maxValue maximum bound for [value], or [Float.POSITIVE_INFINITY] if still unknown
 * @param reverseScrolling for horizontal scroll, when this is `true`, 0 [value] will mean right,
 * when`false`, 0 [value] will mean left. For vertical scroll, when this is `true`, 0 [value] will
 * mean bottom, when `false`, 0 [value] will mean top
 */
class ScrollAxisRange(
    val value: () -> Float,
    val maxValue: () -> Float,
    val reverseScrolling: Boolean = false
) {
    override fun toString(): String =
        "ScrollAxisRange(value=${value()}, maxValue=${maxValue()}, " +
            "reverseScrolling=$reverseScrolling)"
}

/**
 * The type of user interface element. Accessibility services might use this to describe the
 * element or do customizations. Most roles can be automatically resolved by the semantics
 * properties of this element. But some elements with subtle differences need an exact role. If an
 * exact role is not listed, [SemanticsPropertyReceiver.role] should not be set and the framework
 * will automatically resolve it.
 */
@Immutable
@kotlin.jvm.JvmInline
value class Role private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * This element is a button control. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsActions.OnClick]
         */
        val Button = Role(0)
        /**
         * This element is a Checkbox which is a component that represents two states (checked /
         * unchecked). Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Checkbox = Role(1)
        /**
         * This element is a Switch which is a two state toggleable component that provides on/off
         * like options. Associated semantics properties for accessibility:
         * [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Switch = Role(2)
        /**
         * This element is a RadioButton which is a component to represent two states, selected and not
         * selected. Associated semantics properties for accessibility: [SemanticsProperties.Disabled],
         * [SemanticsProperties.StateDescription], [SemanticsActions.OnClick]
         */
        val RadioButton = Role(3)
        /**
         * This element is a Tab which represents a single page of content using a text label and/or
         * icon. A Tab also has two states: selected and not selected. Associated semantics properties
         * for accessibility: [SemanticsProperties.Disabled], [SemanticsProperties.StateDescription],
         * [SemanticsActions.OnClick]
         */
        val Tab = Role(4)
        /**
         * This element is an image. Associated semantics properties for accessibility:
         * [SemanticsProperties.ContentDescription]
         */
        val Image = Role(5)
    }

    override fun toString() = when (this) {
        Button -> "Button"
        Checkbox -> "Checkbox"
        Switch -> "Switch"
        RadioButton -> "RadioButton"
        Tab -> "Tab"
        Image -> "Image"
        else -> "Unknown"
    }
}

/**
 * The mode of live region. Live region indicates to accessibility services they should
 * automatically notify the user about changes to the node's content description or text, or to
 * the content descriptions or text of the node's children (where applicable).
 */
@Immutable
@kotlin.jvm.JvmInline
value class LiveRegionMode private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        /**
         * Live region mode specifying that accessibility services should announce
         * changes to this node.
         */
        val Polite = LiveRegionMode(0)
        /**
         * Live region mode specifying that accessibility services should interrupt
         * ongoing speech to immediately announce changes to this node.
         */
        val Assertive = LiveRegionMode(1)
    }

    override fun toString() = when (this) {
        Polite -> "Polite"
        Assertive -> "Assertive"
        else -> "Unknown"
    }
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
 * If this is not set, accessibility services will present the [text][SemanticsProperties.Text] of
 * this node as the content.
 *
 * This typically should not be set directly by applications, because some screen readers will
 * cease presenting other relevant information when this property is present. This is intended
 * to be used via Foundation components which are inherently intractable to automatically
 * describe, such as Image, Icon, and Canvas.
 */
var SemanticsPropertyReceiver.contentDescription: String
    get() = throwSemanticsGetNotSupported()
    set(value) { set(SemanticsProperties.ContentDescription, listOf(value)) }

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

/**
 * The node is marked as heading for accessibility.
 *
 * @see SemanticsProperties.Heading
 */
fun SemanticsPropertyReceiver.heading() {
    this[SemanticsProperties.Heading] = Unit
}

/**
 * Accessibility-friendly title for a screen's pane. For accessibility purposes, a pane is a
 * visually distinct portion of a window, such as the contents of a open drawer. In order for
 * accessibility services to understand a pane's window-like behavior, you should give
 * descriptive titles to your app's panes. Accessibility services can then provide more granular
 * information to users when a pane's appearance or content changes.
 *
 * @see SemanticsProperties.PaneTitle
 */
var SemanticsPropertyReceiver.paneTitle by SemanticsProperties.PaneTitle

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
 * This node is marked as live region for accessibility. This indicates to accessibility services
 * they should automatically notify the user about changes to the node's content description or
 * text, or to the content descriptions or text of the node's children (where applicable). It
 * should be used with caution, especially with assertive mode which immediately stops the
 * current audio and the user does not hear the rest of the content. An example of proper use is
 * a Snackbar which is marked as [LiveRegionMode.Polite].
 *
 * @see SemanticsProperties.LiveRegion
 * @see LiveRegionMode
 */
var SemanticsPropertyReceiver.liveRegion by SemanticsProperties.LiveRegion

/**
 * Whether this semantics node is focused. The presence of this property indicates this node is
 * focusable
 *
 * @see SemanticsProperties.Focused
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
 * If looking for a way to hide semantics of small items from screen readers because they're
 * redundant with semantics of their parent, consider [SemanticsModifier.clearAndSetSemantics]
 * instead.
 */
@ExperimentalComposeUiApi
fun SemanticsPropertyReceiver.invisibleToUser() {
    this[SemanticsProperties.InvisibleToUser] = Unit
}

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
 */
var SemanticsPropertyReceiver.role by SemanticsProperties.Role

/**
 * Test tag attached to this semantics node.
 *
 * This can be used to find nodes in testing frameworks:
 * - In Compose's built-in unit test framework, use with
 * [onNodeWithTag][androidx.compose.ui.test.onNodeWithTag].
 * - For newer AccessibilityNodeInfo-based integration test frameworks, it can be matched in the
 * extras with key "androidx.compose.ui.semantics.testTag"
 * - For legacy AccessibilityNodeInfo-based integration tests, it's optionally exposed as the
 * resource id if [testTagsAsResourceId] is true (for matching with 'By.res' in UIAutomator).
 */
var SemanticsPropertyReceiver.testTag by SemanticsProperties.TestTag

/**
 * Text of the semantics node. It must be real text instead of developer-set content description.
 *
 * @see SemanticsPropertyReceiver.editableText
 */
var SemanticsPropertyReceiver.text: AnnotatedString
    get() = throwSemanticsGetNotSupported()
    set(value) { set(SemanticsProperties.Text, listOf(value)) }

/**
 * Input text of the text field with visual transformation applied to it. It must be a real text
 * entered by the user with visual transformation applied on top of the input text instead of a
 * developer-set content description.
 */
var SemanticsPropertyReceiver.editableText by SemanticsProperties.EditableText

/**
 * Text selection range for the text field.
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
 * This semantics marks node as a collection and provides the required information.
 *
 * @see collectionItemInfo
 */
var SemanticsPropertyReceiver.collectionInfo by SemanticsProperties.CollectionInfo

/**
 * This semantics marks node as an items of a collection and provides the required information.
 *
 * If you mark items of a collection, you should also be marking the collection with
 * [collectionInfo].
 */
var SemanticsPropertyReceiver.collectionItemInfo by SemanticsProperties.CollectionItemInfo

/**
 * The state of a toggleable component.
 *
 * The presence of this property indicates that the element is toggleable.
 */
var SemanticsPropertyReceiver.toggleableState by SemanticsProperties.ToggleableState

/**
 * The node is marked as a password.
 */
fun SemanticsPropertyReceiver.password() {
    this[SemanticsProperties.Password] = Unit
}

/**
 * Mark semantics node that contains invalid input or error.
 *
 * @param [description] a localized description explaining an error to the accessibility user
 */
fun SemanticsPropertyReceiver.error(description: String) {
    this[SemanticsProperties.Error] = description
}

/**
 * The index of an item identified by a given key. The key is usually defined during the creation
 * of the container. If the key did not match any of the items' keys, the [mapping] must return -1.
 */
fun SemanticsPropertyReceiver.indexForKey(mapping: (Any) -> Int) {
    this[SemanticsProperties.IndexForKey] = mapping
}

/**
 * The node is marked as a collection of horizontally or vertically stacked selectable elements.
 *
 * Unlike [collectionInfo] which marks a collection of any elements and asks developer to
 * provide all the required information like number of elements etc., this semantics will
 * populate the number of selectable elements automatically. Note that if you use this semantics
 * with lazy collections, it won't get the number of elements in the collection.
 *
 * @see SemanticsPropertyReceiver.selected
*/
fun SemanticsPropertyReceiver.selectableGroup() {
    this[SemanticsProperties.SelectableGroup] = Unit
}

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
    action: ((MutableList<TextLayoutResult>) -> Boolean)?
) {
    this[SemanticsActions.GetTextLayoutResult] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is clicked (single-tapped).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnClick] is called.
 */
fun SemanticsPropertyReceiver.onClick(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.OnClick] = AccessibilityAction(label, action)
}

/**
 * Action to be performed when the node is long clicked (long-pressed).
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.OnLongClick] is called.
 */
fun SemanticsPropertyReceiver.onLongClick(label: String? = null, action: (() -> Boolean)?) {
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
    action: ((x: Float, y: Float) -> Boolean)?
) {
    this[SemanticsActions.ScrollBy] = AccessibilityAction(label, action)
}

/**
 * Action to scroll a container to the index of one of its items.
 *
 * The [action] should throw an [IllegalArgumentException] if the index is out of bounds.
 */
fun SemanticsPropertyReceiver.scrollToIndex(
    label: String? = null,
    action: (Int) -> Boolean
) {
    this[SemanticsActions.ScrollToIndex] = AccessibilityAction(label, action)
}

/**
 * Action to set the current value of the progress bar.
 *
 * Expected to be used in conjunction with progressBarRangeInfo.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.SetProgress] is called.
 */
fun SemanticsPropertyReceiver.setProgress(label: String? = null, action: ((Float) -> Boolean)?) {
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
fun SemanticsPropertyReceiver.setText(
    label: String? = null,
    action: ((AnnotatedString) -> Boolean)?
) {
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
    action: ((startIndex: Int, endIndex: Int, traversalMode: Boolean) -> Boolean)?
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
    action: (() -> Boolean)?
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
    action: (() -> Boolean)?
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
    action: (() -> Boolean)?
) {
    this[SemanticsActions.PasteText] = AccessibilityAction(label, action)
}

/**
 * Action to expand an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Expand] is called.
 */
fun SemanticsPropertyReceiver.expand(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.Expand] = AccessibilityAction(label, action)
}

/**
 * Action to collapse an expandable node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Collapse] is called.
 */
fun SemanticsPropertyReceiver.collapse(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.Collapse] = AccessibilityAction(label, action)
}

/**
 * Action to dismiss a dismissible node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.Dismiss] is called.
 */
fun SemanticsPropertyReceiver.dismiss(
    label: String? = null,
    action: (() -> Boolean)?
) {
    this[SemanticsActions.Dismiss] = AccessibilityAction(label, action)
}

/**
 * Action that gives input focus to this node.
 *
 * @param label Optional label for this action.
 * @param action Action to be performed when the [SemanticsActions.RequestFocus] is called.
 */
fun SemanticsPropertyReceiver.requestFocus(label: String? = null, action: (() -> Boolean)?) {
    this[SemanticsActions.RequestFocus] = AccessibilityAction(label, action)
}
