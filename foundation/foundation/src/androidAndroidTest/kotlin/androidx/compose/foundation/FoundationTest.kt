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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.ui.test.ComposeTestRuleJUnit
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.onNodeWithTag

/**
 * Constant to emulate very big but finite constraints
 */
val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

fun ComposeTestRuleJUnit.setContentForSizeAssertions(
    parentMaxWidth: Dp = BigTestMaxWidth,
    parentMaxHeight: Dp = BigTestMaxHeight,
    children: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        Box {
            Box(
                Modifier.preferredSizeIn(maxWidth = parentMaxWidth, maxHeight = parentMaxHeight)
                    .testTag("containerForSizeAssertion")
            ) {
                children()
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion")
}
