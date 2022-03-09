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

package androidx.compose.ui.demos.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val ModifierLocalColor = modifierLocalOf { "Unspecified" }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommunicatingModifierDemo() {
    val scaffoldState = rememberScaffoldState()

    fun Modifier.clickToRead() = composed {
        var name by remember { mutableStateOf("Unknown") }
        val coroutineScope = rememberCoroutineScope()
        Modifier
            .modifierLocalConsumer { name = ModifierLocalColor.current }
            .clickable {
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar(
                        "Consumed value provided by $name Box."
                    )
                }
            }
    }

    Scaffold(scaffoldState = scaffoldState) { innerPadding ->
        Column(
            Modifier
                .background(Gray)
                .padding(innerPadding)
                .modifierLocalProvider(ModifierLocalColor) { "Gray" }
        ) {
            Text("Click the red box to read the parent's ModifierLocalColor")
            Box(
                Modifier
                    .size(100.dp)
                    .padding(5.dp)
                    .background(Red)
                    .clickToRead()
            )
            Text("Click the blue box to read its ModifierLocalColor")
            Box(
                Modifier
                    .size(100.dp)
                    .padding(5.dp)
                    .modifierLocalProvider(ModifierLocalColor) { "Blue" }
                    .background(Blue)
                    .clickToRead()
            )
            Text("Click the blue box to read the red box's ModifierLocalColor")
            Box(
                Modifier
                    .size(100.dp)
                    .padding(5.dp)
                    .background(Red)
                    .modifierLocalProvider(ModifierLocalColor) { "Red" }
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .padding(5.dp)
                        .background(Blue)
                        .clickToRead()
                )
            }
            Text("Click the blue box to read its ModifierLocalColor")
            Box(
                Modifier
                    .size(100.dp)
                    .padding(5.dp)
                    .background(Red)
                    .modifierLocalProvider(ModifierLocalColor) { "Red" }
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .padding(5.dp)
                        .background(Blue)
                        .modifierLocalProvider(ModifierLocalColor) { "Blue" }
                        .clickToRead()
                )
            }
        }
    }
}
