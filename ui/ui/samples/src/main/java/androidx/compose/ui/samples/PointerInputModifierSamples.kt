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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

// Empty for use in sample
@Suppress("UNUSED_PARAMETER")
private fun performAction(parameter: String) {
    // This space for rent
}

@Sampled
fun keyedPointerInputModifier() {
    @Composable
    fun MyComposable(parameter: String) {
        Box(
            Modifier.fillMaxSize()
                .pointerInput(parameter) {
                    // This entire pointerInput block will restart from the beginning
                    // if and when `parameter` changes, since it's used as a key in
                    // the creation of the `pointerInput` modifier
                    detectTapGestures {
                        performAction(parameter)
                    }
                }
        )
    }
}

@Sampled
fun rememberedUpdatedParameterPointerInputModifier() {
    @Composable
    fun MyComposable(parameter: String) {
        val currentParameter by rememberUpdatedState(parameter)
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    // This pointerInput block will never restart since
                    // it specifies a key of `Unit`, which never changes
                    detectTapGestures {
                        // ...however, currentParameter is updated out from under this running
                        // pointerInput suspend block by rememberUpdatedState, and will always
                        // contain the latest value updated by the composition when a tap
                        // is detected here.
                        performAction(currentParameter)
                    }
                }
        )
    }
}