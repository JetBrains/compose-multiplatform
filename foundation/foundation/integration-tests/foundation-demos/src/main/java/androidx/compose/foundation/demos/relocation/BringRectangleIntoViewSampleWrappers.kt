/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.demos.relocation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.samples.BringIntoViewResponderSample
import androidx.compose.foundation.samples.BringPartOfComposableIntoViewSample
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun BringRectangleIntoViewDemo() {
    Column {
        Text(
            "This is a scrollable Box. Drag to scroll the Circle into view or click the " +
                "button to bring the circle into view."
        )
        BringPartOfComposableIntoViewSample()
    }
}

@Composable
fun BringIntoViewResponderDemo() {
    Column {
        Text(
            "Each cell in this box is focusable, use the arrow keys/tab/dpad to move focus " +
                "around. The container will always put the last-requested rectangle in the top-" +
                "left of itself."
        )
        BringIntoViewResponderSample()
    }
}