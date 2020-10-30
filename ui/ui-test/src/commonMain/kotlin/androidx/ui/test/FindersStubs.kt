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

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onAllNodesWithLabel
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithLabel
import androidx.compose.ui.test.onNodeWithSubstring
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onNodeWithTag(testTag, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onNodeWithTag(
    testTag: String,
    useUnmergedTree: Boolean = false
) = onNodeWithTag(testTag, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onAllNodesWithTag(testTag, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onAllNodesWithTag(
    testTag: String,
    useUnmergedTree: Boolean = false
) = onAllNodesWithTag(testTag, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onNodeWithLabel(label, ignoreCase, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onNodeWithLabel(
    label: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onNodeWithLabel(label, ignoreCase, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onNodeWithText(text, ignoreCase, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onNodeWithText(
    text: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onNodeWithText(text, ignoreCase, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onNodeWithSubstring(text, ignoreCase, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onNodeWithSubstring(
    text: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onNodeWithSubstring(text, ignoreCase, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onAllNodesWithText(text, ignoreCase, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onAllNodesWithText(
    text: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onAllNodesWithText(text, ignoreCase, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onAllNodesWithLabel(label, ignoreCase, useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onAllNodesWithLabel(
    label: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onAllNodesWithLabel(label, ignoreCase, useUnmergedTree)

/**
 * @Deprecated Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library
 */
@Deprecated(
    "Moved to androidx.compose.ui.test in androidx.compose.ui:ui-test library",
    replaceWith = ReplaceWith(
        "onRoot(useUnmergedTree)",
        "androidx.compose.ui.test"
    )
)
fun SemanticsNodeInteractionsProvider.onRoot(useUnmergedTree: Boolean = false) =
    onRoot(useUnmergedTree)