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

import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.semantics.AccessibilityRangeInfo
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsHidden
import androidx.compose.ui.test.assertIsInMutuallyExclusiveGroup
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotHidden
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertLabelEquals
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertValueEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Dp

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsHidden()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsHidden() = assertIsHidden()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsNotHidden()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsNotHidden() = assertIsNotHidden()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsDisplayed()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsDisplayed() = assertIsDisplayed()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsNotDisplayed()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsNotDisplayed() =
    assertIsNotDisplayed()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsEnabled()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsEnabled() = assertIsEnabled()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsNotEnabled()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsNotEnabled() =
    assertIsNotEnabled()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsOn()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsOn() = assertIsOn()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsOff()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsOff() = assertIsOff()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsSelected()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsSelected() = assertIsSelected()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsNotSelected()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsNotSelected() =
    assertIsNotSelected()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsToggleable()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsToggleable() =
    assertIsToggleable()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsSelectable()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsSelectable() =
    assertIsSelectable()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertIsInMutuallyExclusiveGroup()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertIsInMutuallyExclusiveGroup() =
    assertIsInMutuallyExclusiveGroup()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertLabelEquals()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertLabelEquals(value: String) =
    assertLabelEquals(value)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertTextEquals()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertTextEquals(value: String) =
    assertTextEquals(value)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertValueEquals()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertValueEquals(value: String) =
    assertValueEquals(value)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertRangeInfoEquals()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertRangeInfoEquals(value: AccessibilityRangeInfo) =
    assertRangeInfoEquals(value)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertHasClickAction()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertHasClickAction() =
    assertHasClickAction()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertHasNoClickAction()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertHasNoClickAction() =
    assertHasNoClickAction()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assert(matcher, messagePrefixOnError)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assert(
    matcher: SemanticsMatcher,
    messagePrefixOnError: (() -> String)? = null
) = assert(matcher, messagePrefixOnError)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertCountEquals(expectedSize)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.assertCountEquals(
    expectedSize: Int
) = assertCountEquals(expectedSize)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertAny(matcher)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.assertAny(
    matcher: SemanticsMatcher
) = assertAny(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertAll(matcher)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteractionCollection.assertAll(
    matcher: SemanticsMatcher
) = assertAll(matcher)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertWidthIsEqualTo(expectedWidth)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertWidthIsEqualTo(expectedWidth: Dp) =
    assertWidthIsEqualTo(expectedWidth)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertHeightIsEqualTo(expectedHeight)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertHeightIsEqualTo(expectedHeight: Dp) =
    assertHeightIsEqualTo(expectedHeight)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("assertWidthIsAtLeast(expectedMinWidth)", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.assertWidthIsAtLeast(expectedMinWidth: Dp) =
    assertWidthIsAtLeast(expectedMinWidth)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "assertHeightIsAtLeast(expectedMinHeight)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteraction.assertHeightIsAtLeast(
    expectedMinHeight: Dp
) = assertHeightIsAtLeast(expectedMinHeight)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith("getUnclippedBoundsInRoot()", "androidx.compose.ui.test")
)
fun SemanticsNodeInteraction.getUnclippedBoundsInRoot() =
    getUnclippedBoundsInRoot()

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "assertPositionInRootIsEqualTo(expectedLeft, expectedTop)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteraction.assertPositionInRootIsEqualTo(
    expectedLeft: Dp,
    expectedTop: Dp
) = assertPositionInRootIsEqualTo(expectedLeft, expectedTop)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "assertTopPositionInRootIsEqualTo(expectedTop)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteraction.assertTopPositionInRootIsEqualTo(
    expectedTop: Dp
) = assertTopPositionInRootIsEqualTo(expectedTop)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "assertLeftPositionInRootIsEqualTo(expectedTop)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteraction.assertLeftPositionInRootIsEqualTo(
    expectedTop: Dp
) = assertLeftPositionInRootIsEqualTo(expectedTop)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "getAlignmentLinePosition(line)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteraction.getAlignmentLinePosition(line: AlignmentLine) =
    getAlignmentLinePosition(line)