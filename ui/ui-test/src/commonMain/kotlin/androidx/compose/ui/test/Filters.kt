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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.fastAny

/**
 * Returns whether the node is enabled.
 *
 * @see SemanticsProperties.Disabled
 */
fun isEnabled(): SemanticsMatcher =
    !hasKey(SemanticsProperties.Disabled)

/**
 * Returns whether the node is not enabled.
 *
 * @see SemanticsProperties.Disabled
 */
fun isNotEnabled(): SemanticsMatcher =
    hasKey(SemanticsProperties.Disabled)

/**
 * Return whether the node is checkable.
 *
 * @see SemanticsProperties.ToggleableState
 */
fun isToggleable(): SemanticsMatcher =
    hasKey(SemanticsProperties.ToggleableState)

/**
 * Returns whether the node is toggled.
 *
 * @see SemanticsProperties.ToggleableState
 */
fun isOn(): SemanticsMatcher = SemanticsMatcher.expectValue(
    SemanticsProperties.ToggleableState, ToggleableState.On
)

/**
 * Returns whether the node is not toggled.
 *
 * @see SemanticsProperties.ToggleableState
 */
fun isOff(): SemanticsMatcher = SemanticsMatcher.expectValue(
    SemanticsProperties.ToggleableState, ToggleableState.Off
)

/**
 * Return whether the node is selectable.
 *
 * @see SemanticsProperties.Selected
 */
fun isSelectable(): SemanticsMatcher =
    hasKey(SemanticsProperties.Selected)

/**
 * Returns whether the node is selected.
 *
 * @see SemanticsProperties.Selected
 */
fun isSelected(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

/**
 * Returns whether the node is not selected.
 *
 * @see SemanticsProperties.Selected
 */
fun isNotSelected(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Selected, false)

/**
 * Return whether the node is able to receive focus
 *
 * @see SemanticsProperties.Focused
 */
fun isFocusable(): SemanticsMatcher =
    hasKey(SemanticsProperties.Focused)

/**
 * Return whether the node is not able to receive focus.
 *
 * @see SemanticsProperties.Focused
 */
fun isNotFocusable(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused)

/**
 * Returns whether the node is focused.
 *
 * @see SemanticsProperties.Focused
 */
fun isFocused(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Focused, true)

/**
 * Returns whether the node is not focused.
 *
 * @see SemanticsProperties.Focused
 */
fun isNotFocused(): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Focused, false)

/**
 * Return whether the node has a semantics click action defined.
 *
 * @see SemanticsActions.OnClick
 */
fun hasClickAction(): SemanticsMatcher =
    hasKey(SemanticsActions.OnClick)

/**
 * Return whether the node has no semantics click action defined.
 *
 * @see SemanticsActions.OnClick
 */
fun hasNoClickAction(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick)

/**
 * Return whether the node has a semantics scrollable action defined.
 *
 * @see SemanticsActions.ScrollBy
 */
fun hasScrollAction(): SemanticsMatcher =
    hasKey(SemanticsActions.ScrollBy)

/**
 * Return whether the node has no semantics scrollable action defined.
 *
 * @see SemanticsActions.ScrollBy
 */
fun hasNoScrollAction(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsActions.ScrollBy)

/**
 * Returns whether the node's content description contains the given [value].
 *
 * Note that in merged semantics tree there can be a list of content descriptions that got merged
 * from the child nodes. Typically an accessibility tooling will decide based on its heuristics
 * which ones to announce.
 *
 * @param value Value to match as one of the items in the list of content descriptions.
 * @param substring Whether to use substring matching.
 * @param ignoreCase Whether case should be ignored.
 *
 * @see SemanticsProperties.ContentDescription
 */
fun hasContentDescription(
    value: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): SemanticsMatcher {
    return if (substring) {
        SemanticsMatcher(
            "${SemanticsProperties.ContentDescription.name} contains '$value' " +
                "(ignoreCase: $ignoreCase)"
        ) {
            it.config.getOrNull(SemanticsProperties.ContentDescription)
                ?.any { item -> item.contains(value, ignoreCase) } ?: false
        }
    } else {
        SemanticsMatcher(
            "${SemanticsProperties.ContentDescription.name} = '$value' (ignoreCase: $ignoreCase)"
        ) {
            it.config.getOrNull(SemanticsProperties.ContentDescription)
                ?.any { item -> item.equals(value, ignoreCase) } ?: false
        }
    }
}

/**
 * Returns whether the node's content description contains exactly the given [values] and nothing
 * else.
 *
 * Note that in merged semantics tree there can be a list of content descriptions that got merged
 * from the child nodes. Typically an accessibility tooling will decide based on its heuristics
 * which ones to announce.
 *
 * @param values List of values to match (the order does not matter)
 *
 * @see SemanticsProperties.ContentDescription
 */
fun hasContentDescriptionExactly(
    vararg values: String
): SemanticsMatcher {
    val expected = values.toList()
    return SemanticsMatcher(
        "${SemanticsProperties.ContentDescription.name} = " +
            "[${values.joinToString(",")}]"
    ) { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.let { given ->
                given.size == expected.size &&
                    given.containsAll(expected) && expected.containsAll(given)
            } ?: values.isEmpty()
    }
}

/**
 * Returns whether the node's text contains the given [text].
 *
 * This will also search in [SemanticsProperties.EditableText].
 *
 * Note that in merged semantics tree there can be a list of text items that got merged from
 * the child nodes. Typically an accessibility tooling will decide based on its heuristics which
 * ones to use.
 *
 * @param text Value to match as one of the items in the list of text values.
 * @param substring Whether to use substring matching.
 * @param ignoreCase Whether case should be ignored.
 *
 * @see SemanticsProperties.Text
 * @see SemanticsProperties.EditableText
 */
fun hasText(
    text: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): SemanticsMatcher {
    val propertyName = "${SemanticsProperties.Text.name} + ${SemanticsProperties.EditableText.name}"
    return if (substring) {
        SemanticsMatcher(
            "$propertyName contains '$text' (ignoreCase: $ignoreCase) as substring"
        ) {
            val isInEditableTextValue = it.config.getOrNull(SemanticsProperties.EditableText)
                ?.text?.contains(text, ignoreCase) ?: false
            val isInTextValue = it.config.getOrNull(SemanticsProperties.Text)
                ?.any { item -> item.text.contains(text, ignoreCase) } ?: false
            isInEditableTextValue || isInTextValue
        }
    } else {
        SemanticsMatcher(
            "$propertyName contains '$text' (ignoreCase: $ignoreCase)"
        ) {
            val isInEditableTextValue = it.config.getOrNull(SemanticsProperties.EditableText)
                ?.text?.equals(text, ignoreCase) ?: false
            val isInTextValue = it.config.getOrNull(SemanticsProperties.Text)
                ?.any { item -> item.text.equals(text, ignoreCase) } ?: false
            isInEditableTextValue || isInTextValue
        }
    }
}

/**
 * Returns whether the node's text contains exactly the given [textValues] and nothing else.
 *
 * This will also search in [SemanticsProperties.EditableText] by default.
 *
 * Note that in merged semantics tree there can be a list of text items that got merged from
 * the child nodes. Typically an accessibility tooling will decide based on its heuristics which
 * ones to use.
 *
 * @param textValues List of values to match (the order does not matter)
 * @param includeEditableText Whether to also assert against the editable text
 *
 * @see SemanticsProperties.Text
 * @see SemanticsProperties.EditableText
 */
fun hasTextExactly(
    vararg textValues: String,
    includeEditableText: Boolean = true
): SemanticsMatcher {
    val expected = textValues.toList()
    val propertyName = if (includeEditableText) {
        "${SemanticsProperties.Text.name} + ${SemanticsProperties.EditableText.name}"
    } else {
        SemanticsProperties.Text.name
    }
    return SemanticsMatcher(
        "$propertyName = [${textValues.joinToString(",")}]"
    ) { node ->
        val actual = mutableListOf<String>()
        if (includeEditableText) {
            node.config.getOrNull(SemanticsProperties.EditableText)
                ?.let { actual.add(it.text) }
        }
        node.config.getOrNull(SemanticsProperties.Text)
            ?.let { actual.addAll(it.map { anStr -> anStr.text }) }
        actual.containsAll(expected) && expected.containsAll(actual)
    }
}

/**
 * Returns whether the node's value matches exactly to the given accessibility value.
 *
 * @param value Value to match.
 *
 * @see SemanticsProperties.StateDescription
 */
fun hasStateDescription(value: String): SemanticsMatcher = SemanticsMatcher.expectValue(
    SemanticsProperties.StateDescription, value
)

/**
 * Returns whether the node is marked as an accessibility header.
 *
 * @see SemanticsProperties.Heading
 */
fun isHeading(): SemanticsMatcher =
    hasKey(SemanticsProperties.Heading)

/**
 * Returns whether the node's range info matches exactly to the given accessibility range info.
 *
 * @param rangeInfo range info to match.
 *
 * @see SemanticsProperties.ProgressBarRangeInfo
 */
fun hasProgressBarRangeInfo(rangeInfo: ProgressBarRangeInfo): SemanticsMatcher = SemanticsMatcher
    .expectValue(SemanticsProperties.ProgressBarRangeInfo, rangeInfo)

/**
 * Returns whether the node is annotated by the given test tag.
 *
 * @param testTag Value to match.
 *
 * @see SemanticsProperties.TestTag
 */
fun hasTestTag(testTag: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.TestTag, testTag)

/**
 * Returns whether the node is a dialog.
 *
 * This only checks if the node itself is a dialog, not if it is _part of_ a dialog. Use
 * `hasAnyAncestorThat(isDialog())` for that.
 *
 * @see SemanticsProperties.IsDialog
 */
fun isDialog(): SemanticsMatcher =
    hasKey(SemanticsProperties.IsDialog)

/**
 * Returns whether the node is a popup.
 *
 * This only checks if the node itself is a popup, not if it is _part of_ a popup. Use
 * `hasAnyAncestorThat(isPopup())` for that.
 *
 * @see SemanticsProperties.IsPopup
 */
fun isPopup(): SemanticsMatcher =
    hasKey(SemanticsProperties.IsPopup)

/**
 * Returns whether the node defines the given IME action.
 *
 * @param actionType the action to match.
 */
fun hasImeAction(actionType: ImeAction) =
    SemanticsMatcher.expectValue(SemanticsProperties.ImeAction, actionType)

/**
 * Returns whether the node defines semantics action to set text to it.
 *
 * This can be used to for instance filter out text fields.
 *
 * @see SemanticsActions.SetText
 */
fun hasSetTextAction() =
    hasKey(SemanticsActions.SetText)

/**
 * Returns whether the node defines the ability to scroll to an item index.
 *
 * Note that not all scrollable containers have item indices. For example, a
 * [scrollable][androidx.compose.foundation.gestures.scrollable] doesn't have items with an
 * index, while [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] does.
 */
fun hasScrollToIndexAction() =
    hasKey(SemanticsActions.ScrollToIndex)

/**
 * Returns whether the node defines the ability to scroll to an item identified by a key, such as
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn] or
 * [LazyRow][androidx.compose.foundation.lazy.LazyRow].
 */
fun hasScrollToKeyAction() =
    hasKey(SemanticsActions.ScrollToIndex)
        .and(hasKey(SemanticsProperties.IndexForKey))

/**
 * Returns whether the node defines the ability to scroll to content identified by a matcher.
 */
fun hasScrollToNodeAction() =
    hasKey(SemanticsActions.ScrollToIndex)
        .and(hasKey(SemanticsActions.ScrollBy))
        .and(
            hasKey(SemanticsProperties.HorizontalScrollAxisRange)
                .or(hasKey(SemanticsProperties.VerticalScrollAxisRange))
        )

/**
 * Return whether the node is the root semantics node.
 *
 * There is always one root in every node tree, added implicitly by Compose.
 */
fun isRoot() =
    SemanticsMatcher("isRoot") { it.isRoot }

/**
 * Returns whether the node's parent satisfies the given matcher.
 *
 * Returns false if no parent exists.
 */
fun hasParent(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the parent's node semantics
    //  in the error message or say that no parent was found.
    return SemanticsMatcher("hasParentThat(${matcher.description})") {
        it.parent?.run { matcher.matches(this) } ?: false
    }
}

/**
 * Returns whether the node has at least one child that satisfies the given matcher.
 */
fun hasAnyChild(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the children nodes semantics
    //  in the error message or say that no children were found.
    return SemanticsMatcher("hasAnyChildThat(${matcher.description})") {
        matcher.matchesAny(it.children)
    }
}

/**
 * Returns whether the node has at least one sibling that satisfies the given matcher.
 *
 * Sibling is defined as a any other node that shares the same parent.
 */
fun hasAnySibling(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the sibling nodes semantics
    //  in the error message or say that no siblings were found.
    return SemanticsMatcher(
        "hasAnySiblingThat(${matcher.description})"
    ) {
        val node = it
        it.parent?.run { matcher.matchesAny(this.children.filter { child -> child.id != node.id }) }
            ?: false
    }
}

/**
 * Returns whether the node has at least one ancestor that satisfies the given matcher.
 *
 * Example: For the following tree
 * ```
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * ```
 * In case of C1, we would check the matcher against A and B
 */
fun hasAnyAncestor(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we should print the ancestor nodes semantics
    //  in the error message or say that no ancestors were found.
    return SemanticsMatcher("hasAnyAncestorThat(${matcher.description})") {
        matcher.matchesAny(it.ancestors)
    }
}

/**
 * Returns whether the node has at least one descendant that satisfies the given matcher.
 *
 * Example: For the following tree
 * ```
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * ```
 * In case of A, we would check the matcher against B,C1 and C2
 */
fun hasAnyDescendant(matcher: SemanticsMatcher): SemanticsMatcher {
    // TODO(b/150292800): If this is used in assert we could consider printing the whole subtree but
    //  it might be too much to show. But we could at least warn if there were no ancestors found.
    fun checkIfSubtreeMatches(matcher: SemanticsMatcher, node: SemanticsNode): Boolean {
        if (matcher.matchesAny(node.children)) {
            return true
        }

        return node.children.fastAny { checkIfSubtreeMatches(matcher, it) }
    }

    return SemanticsMatcher("hasAnyDescendantThat(${matcher.description})") {
        checkIfSubtreeMatches(matcher, it)
    }
}

internal val SemanticsNode.ancestors: Iterable<SemanticsNode>
    get() = object : Iterable<SemanticsNode> {
        override fun iterator(): Iterator<SemanticsNode> {
            return object : Iterator<SemanticsNode> {
                var next = parent
                override fun hasNext(): Boolean {
                    return next != null
                }

                override fun next(): SemanticsNode {
                    return next!!.also { next = it.parent }
                }
            }
        }
    }

private fun hasKey(key: SemanticsPropertyKey<*>): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(key)
