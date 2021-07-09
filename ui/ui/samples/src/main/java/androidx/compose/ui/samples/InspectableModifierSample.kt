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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun InspectableModifierSample() {

    /**
     * Sample with a single parameter
     */
    fun Modifier.simpleFrame(color: Color) = inspectable(
        inspectorInfo = debugInspectorInfo {
            name = "simpleFrame"
            value = color
        }
    ) {
        background(color, RoundedCornerShape(5.0.dp))
    }

    /**
     * Sample with multiple parameters
     */
    fun Modifier.fancyFrame(size: Dp, color: Color) = inspectable(
        inspectorInfo = debugInspectorInfo {
            name = "fancyFrame"
            properties["size"] = size
            properties["color"] = color
        }
    ) {
        background(color, RoundedCornerShape(size))
    }
}
