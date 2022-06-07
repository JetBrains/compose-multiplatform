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

package androidx.compose.foundation.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.samples.FocusGroupSample
import androidx.compose.foundation.samples.FocusableFocusGroupSample
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun FocusGroupDemo() {
    Column {
        Text(
            """
            Use the tab key to move focus among buttons.
            Notice how all the items in column 1 are visited before focus moves to the
            second column. This is because each column is in its own focus group.
            """.trimIndent()
        )
        FocusGroupSample()
        Text(
            """
            The LazyRow on the bottom is an example of a focuable focus group. Notice
            how the parent gains focus first and you have to use DPad center or the Tab
            key to visit the children inside the LazyRow
            """.trimIndent()
        )
        FocusableFocusGroupSample()
    }
}
