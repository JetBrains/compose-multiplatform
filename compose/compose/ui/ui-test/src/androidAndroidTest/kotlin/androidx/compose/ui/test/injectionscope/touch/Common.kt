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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.ui.test.MultiModalInjectionScope
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMultiModalInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag

object Common {
    fun ComposeTestRule.performTouchInput(block: TouchInjectionScope.() -> Unit) {
        onNodeWithTag(defaultTag).performTouchInput(block)
    }

    fun ComposeTestRule.performMultiModalInput(block: MultiModalInjectionScope.() -> Unit) {
        onNodeWithTag(defaultTag).performMultiModalInput(block)
    }
}
