/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier

/**
 * Component that represents an empty space layout, whose size can be defined using
 * [Modifier.width], [Modifier.height] and [Modifier.size] modifiers.
 *
 * @sample androidx.compose.foundation.layout.samples.SpacerExample
 *
 * @param modifier modifiers to set to this spacer
 */
@Composable
fun Spacer(modifier: Modifier) {
    Layout({}, modifier) { _, constraints ->
        with(constraints) {
            val width = if (hasFixedWidth) maxWidth else 0
            val height = if (hasFixedHeight) maxHeight else 0
            layout(width, height) {}
        }
    }
}