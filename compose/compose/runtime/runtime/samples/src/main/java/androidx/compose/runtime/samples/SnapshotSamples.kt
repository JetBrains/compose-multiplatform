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
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.asContextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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

@Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
private suspend fun doSomethingSuspending(param: Any?): Any? = null

@Suppress("ClassName")
private object someObject {
    var stateA by mutableStateOf(0)
    var stateB by mutableStateOf(0)
}

@Suppress("unused")
@OptIn(ExperimentalComposeApi::class)
@Sampled
fun snapshotAsContextElementSample() {
    runBlocking {
        val snapshot = Snapshot.takeSnapshot()
        try {
            withContext(snapshot.asContextElement()) {
                // Data observed by separately reading stateA and stateB are consistent with
                // the snapshot context element across suspensions
                doSomethingSuspending(someObject.stateA)
                doSomethingSuspending(someObject.stateB)
            }
        } finally {
            // Snapshot must be disposed after it will not be used again
            snapshot.dispose()
        }
    }
}