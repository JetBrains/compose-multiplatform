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

import androidx.compose.ui.test.onAllNodesWithLabel
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithLabel
import androidx.compose.ui.test.onNodeWithSubstring
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onNodeWithTag(
    testTag: String,
    useUnmergedTree: Boolean = false
) = onNodeWithTag(testTag, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onAllNodesWithTag(
    testTag: String,
    useUnmergedTree: Boolean = false
) = onAllNodesWithTag(testTag, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onNodeWithLabel(
    label: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onNodeWithLabel(label, ignoreCase, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onNodeWithText(
    text: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onNodeWithText(text, ignoreCase, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onNodeWithSubstring(
    text: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onNodeWithSubstring(text, ignoreCase, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onAllNodesWithText(
    text: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onAllNodesWithText(text, ignoreCase, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onAllNodesWithLabel(
    label: String,
    ignoreCase: Boolean = false,
    useUnmergedTree: Boolean = false
) = onAllNodesWithLabel(label, ignoreCase, useUnmergedTree)

/** @Deprecated Moved to androidx.compose.ui.test */
fun ComposeTestRule.onRoot(useUnmergedTree: Boolean = false) = onRoot(useUnmergedTree)