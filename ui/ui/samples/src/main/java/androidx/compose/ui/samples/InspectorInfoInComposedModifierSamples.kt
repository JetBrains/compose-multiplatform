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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp

@Suppress("UnnecessaryComposedModifier")
@Composable
@Sampled
fun InspectorInfoInComposedModifierSample() {

    // let's create you own custom stateful modifier
    fun Modifier.myColorModifier(color: Color) = composed(
        // pass inspector information for debug
        inspectorInfo = debugInspectorInfo {
            // name should match the name of the modifier
            name = "myColorModifier"
            // specify a single argument as the value when the argument name is irrelevant
            value = color
        },
        // pass your modifier implementation that resolved per modified element
        factory = {
            // add your modifier implementation here
            Modifier
        }
    )
}

@Suppress("UnnecessaryComposedModifier")
@Composable
@Sampled
fun InspectorInfoInComposedModifierWithArgumentsSample() {

    // let's create you own custom stateful modifier with multiple arguments
    fun Modifier.myModifier(width: Dp, height: Dp, color: Color) = composed(
        // pass inspector information for debug
        inspectorInfo = debugInspectorInfo {
            // name should match the name of the modifier
            name = "myModifier"
            // add name and value of each argument
            properties["width"] = width
            properties["height"] = height
            properties["color"] = color
        },
        // pass your modifier implementation that resolved per modified element
        factory = {
            // add your modifier implementation here
            Modifier
        }
    )
}
