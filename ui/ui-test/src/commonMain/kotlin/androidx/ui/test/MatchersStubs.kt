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

package androidx.ui.test

import androidx.compose.ui.semantics.AccessibilityRangeInfo
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.input.ImeAction

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isEnabled()", "androidx.compose.ui.test")
)
fun isEnabled() = androidx.compose.ui.test.isEnabled()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isNotEnabled()", "androidx.compose.ui.test")
)
fun isNotEnabled() = androidx.compose.ui.test.isNotEnabled()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isToggleable()", "androidx.compose.ui.test")
)
fun isToggleable() = androidx.compose.ui.test.isToggleable()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isOn()", "androidx.compose.ui.test")
)
fun isOn() = androidx.compose.ui.test.isOn()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isOff()", "androidx.compose.ui.test")
)
fun isOff() = androidx.compose.ui.test.isOff()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isSelectable()", "androidx.compose.ui.test")
)
fun isSelectable() = androidx.compose.ui.test.isSelectable()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isSelected()", "androidx.compose.ui.test")
)
fun isSelected() = androidx.compose.ui.test.isSelected()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isNotSelected()", "androidx.compose.ui.test")
)
fun isNotSelected() = androidx.compose.ui.test.isNotSelected()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isFocused()", "androidx.compose.ui.test")
)
fun isFocused() = androidx.compose.ui.test.isFocused()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isNotFocused()", "androidx.compose.ui.test")
)
fun isNotFocused() = androidx.compose.ui.test.isNotFocused()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasClickAction()", "androidx.compose.ui.test")
)
fun hasClickAction() = androidx.compose.ui.test.hasClickAction()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasNoClickAction()", "androidx.compose.ui.test")
)
fun hasNoClickAction() = androidx.compose.ui.test.hasNoClickAction()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasScrollAction()", "androidx.compose.ui.test")
)
fun hasScrollAction() = androidx.compose.ui.test.hasScrollAction()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasNoScrollAction()", "androidx.compose.ui.test")
)
fun hasNoScrollAction() = androidx.compose.ui.test.hasNoScrollAction()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasLabel(label, ignoreCase)", "androidx.compose.ui.test")
)
fun hasLabel(label: String, ignoreCase: Boolean = false) =
    androidx.compose.ui.test.hasLabel(label, ignoreCase)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasText(text, ignoreCase)", "androidx.compose.ui.test")
)
fun hasText(text: String, ignoreCase: Boolean = false) =
    androidx.compose.ui.test.hasText(text, ignoreCase)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasSubstring(substring, ignoreCase)", "androidx.compose.ui.test")
)
fun hasSubstring(substring: String, ignoreCase: Boolean = false) =
    androidx.compose.ui.test.hasSubstring(substring, ignoreCase)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasValue(value)", "androidx.compose.ui.test")
)
fun hasValue(value: String) = androidx.compose.ui.test.hasValue(value)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasRangeInfo(rangeInfo)", "androidx.compose.ui.test")
)
fun hasRangeInfo(rangeInfo: AccessibilityRangeInfo) =
    androidx.compose.ui.test.hasRangeInfo(rangeInfo)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasTestTag(testTag)", "androidx.compose.ui.test")
)
fun hasTestTag(testTag: String) = androidx.compose.ui.test.hasTestTag(testTag)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isInMutuallyExclusiveGroup()", "androidx.compose.ui.test")
)
fun isInMutuallyExclusiveGroup() = androidx.compose.ui.test.isInMutuallyExclusiveGroup()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isHidden()", "androidx.compose.ui.test")
)
fun isHidden() = androidx.compose.ui.test.isHidden()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isNotHidden()", "androidx.compose.ui.test")
)
fun isNotHidden() = androidx.compose.ui.test.isNotHidden()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isDialog()", "androidx.compose.ui.test")
)
fun isDialog() = androidx.compose.ui.test.isDialog()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isPopup()", "androidx.compose.ui.test")
)
fun isPopup() = androidx.compose.ui.test.isPopup()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasImeAction()", "androidx.compose.ui.test")
)
fun hasImeAction(actionType: ImeAction) = androidx.compose.ui.test.hasImeAction(actionType)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasInputMethodsSupport()", "androidx.compose.ui.test")
)
fun hasInputMethodsSupport() = androidx.compose.ui.test.hasInputMethodsSupport()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("isRoot()", "androidx.compose.ui.test")
)
fun isRoot() = androidx.compose.ui.test.isRoot()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasParent()", "androidx.compose.ui.test")
)
fun hasParent(matcher: SemanticsMatcher) = androidx.compose.ui.test.hasParent(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasAnyChild(matcher)", "androidx.compose.ui.test")
)
fun hasAnyChild(matcher: SemanticsMatcher): SemanticsMatcher =
    androidx.compose.ui.test.hasAnyChild(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasAnySibling(matcher)", "androidx.compose.ui.test")
)
fun hasAnySibling(matcher: SemanticsMatcher): SemanticsMatcher =
    androidx.compose.ui.test.hasAnySibling(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasAnyAncestor(matcher)", "androidx.compose.ui.test")
)
fun hasAnyAncestor(matcher: SemanticsMatcher) = androidx.compose.ui.test.hasAnyAncestor(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("hasAnyDescendant(matcher)", "androidx.compose.ui.test")
)
fun hasAnyDescendant(matcher: SemanticsMatcher) = androidx.compose.ui.test.hasAnyDescendant(matcher)