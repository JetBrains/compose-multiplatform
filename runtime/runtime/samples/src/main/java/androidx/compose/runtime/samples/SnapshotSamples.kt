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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Suppress("UNREACHABLE_CODE", "CanBeVal", "UNUSED_VARIABLE")
@Sampled
fun snapshotFlowSample() {
    // Define Snapshot state objects
    var greeting by mutableStateOf("Hello")
    var person by mutableStateOf("Adam")

    // ...

    // Create a flow that will emit whenever our person-specific greeting changes
    val greetPersonFlow = snapshotFlow { "$greeting, $person" }

    // ...

    val collectionScope: CoroutineScope = TODO("Use your scope here")

    // Collect the flow and offer greetings!
    collectionScope.launch {
        greetPersonFlow.collect {
            println(greeting)
        }
    }

    // ...

    // Change snapshot state; greetPersonFlow will emit a new greeting
    Snapshot.withMutableSnapshot {
        greeting = "Ahoy"
        person = "Sean"
    }
}
