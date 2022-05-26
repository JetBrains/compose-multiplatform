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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun OnGloballyPositioned() {
    Column(
        Modifier.onGloballyPositioned { coordinates ->
            // This will be the size of the Column.
            coordinates.size
            // The position of the Column relative to the application window.
            coordinates.positionInWindow()
            // The position of the Column relative to the Compose root.
            coordinates.positionInRoot()
            // These will be the alignment lines provided to the layout (empty here for Column).
            coordinates.providedAlignmentLines
            // This will be a LayoutCoordinates instance corresponding to the parent of Column.
            coordinates.parentLayoutCoordinates
        }
    ) {
        Box(Modifier.size(20.dp).background(Color.Green))
        Box(Modifier.size(20.dp).background(Color.Blue))
    }
}
