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

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Sampled
@Composable
fun FlowWithInitialSample(flow: Flow<String>) {
    val value: String by flow.collectAsState("initial")
    Text("Value is $value")
}

@Sampled
@Composable
fun StateFlowSample(stateFlow: StateFlow<String>) {
    val value: String by stateFlow.collectAsState()
    Text("Value is $value")
}