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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.ui.test.ComposeTestRule
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp

/**
 * Constant to emulate very big but finite constraints
 */
val BigTestConstraints = DpConstraints(maxWidth = 5000.dp, maxHeight = 5000.dp)

fun ComposeTestRule.setContentForSizeAssertions(
    parentConstraints: DpConstraints = BigTestConstraints,
    children: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        Stack {
            Stack(
                Modifier.preferredSizeIn(parentConstraints)
                    .testTag("containerForSizeAssertion")) {
                children()
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion")
}
