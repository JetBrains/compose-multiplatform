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

// Suppress on a file level because we can't suppress the warning for the import statement
@file:Suppress("DEPRECATION")

package androidx.compose.ui.test.partialgesturescope

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.GestureScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag

object Common {
    fun ComposeTestRule.partialGesture(block: GestureScope.() -> Unit) {
        onNodeWithTag(defaultTag).performGesture(block)
    }
}
