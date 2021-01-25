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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.tooling

import androidx.compose.animation.DpPropKey
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.transition
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val CheckBoxCorner = DpPropKey("CheckBox Corner")
enum class CheckBoxState { Unselected, Selected }
val CheckBoxTransitionDefinition = transitionDefinition<CheckBoxState> {
    state(CheckBoxState.Selected) {
        this[CheckBoxCorner] = 28.dp
    }
    state(CheckBoxState.Unselected) {
        this[CheckBoxCorner] = 0.dp
    }
}

@Preview("Single CheckBox")
@Composable
fun CheckBoxPreview() {
    CheckBox()
}

@Preview(name = "CheckBox + Scaffold")
@Composable
fun CheckBoxScaffoldPreview() {
    Scaffold {
        CheckBox()
    }
}

@Composable
private fun CheckBox() {
    val (selected, onSelected) = remember { mutableStateOf(false) }
    val state = transition(
        label = "checkBoxAnim",
        definition = CheckBoxTransitionDefinition,
        toState = if (selected) CheckBoxState.Selected else CheckBoxState.Unselected
    )
    Surface(
        shape = MaterialTheme.shapes.large.copy(topLeft = CornerSize(state[CheckBoxCorner])),
        modifier = Modifier.toggleable(value = selected, onValueChange = onSelected)
    ) {
        Icon(imageVector = Icons.Filled.Done, contentDescription = null)
    }
}
