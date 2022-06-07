/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp

@Sampled
fun clickAndVerifyCheckbox() {
    composeTestRule.onNode(isToggleable())
        .performClick()
        .assertIsOn()
}

@Sampled
fun useUnmergedTree() {
    composeTestRule.setContent {
        // Box is a semantically merging composable. All testTags of its
        // children are merged up into it in the merged semantics tree.
        Box(Modifier.testTag("box").padding(16.dp)) {
            Box(Modifier.testTag("icon").size(48.dp))
        }
    }

    // Verify the position of the inner box. Without `useUnmergedTree`, the
    // test would check the position of the outer box (which is `(0, 0)`)
    // instead of the position of the inner box (which is `(16, 16)`).
    composeTestRule.onNodeWithTag("icon", useUnmergedTree = true)
        .assertLeftPositionInRootIsEqualTo(16.dp)
        .assertTopPositionInRootIsEqualTo(16.dp)
}

@Sampled
fun verifyTwoClickableNodes() {
    composeTestRule.onAllNodes(hasClickAction())
        .assertCountEquals(2)
}
