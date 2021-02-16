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

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Suppress("unused")
@Sampled
fun observeUserSample() {
    @Composable
    fun observeUser(userId: Int): User? {
        val user = remember(userId) { mutableStateOf<User?>(null) }
        DisposableEffect(userId) {
            val subscription = UserAPI.subscribeToUser(userId) {
                user.value = it
            }
            onDispose {
                subscription.unsubscribe()
            }
        }
        return user.value
    }
}

@Sampled
@Composable
fun TwoInputsKeySample() {
    for (element in elements) {
        val selected by key(element.id, parentId) { remember { mutableStateOf(false) } }
        ListItem(item = element, selected = selected)
    }
}

@Sampled
@Composable
fun SimpleStateSample() {
    val count = remember { mutableStateOf(0) }

    Text(text = "You clicked ${count.value} times")
    Button(onClick = { count.value++ }) {
        Text("Click me")
    }
}

@Sampled
@Composable
fun DestructuredStateSample() {
    val (count, setCount) = remember { mutableStateOf(0) }

    Text(text = "You clicked $count times")
    Button(onClick = { setCount(count + 1) }) {
        Text("Click me")
    }
}

@Sampled
@Composable
fun DelegatedReadOnlyStateSample() {
    // Composable function that manages a subscription to a data source, returning it as State
    @Composable
    fun observeSampleData(): State<String> = TODO()

    // Subscription is managed here, but currentValue is not read yet
    val currentValue by observeSampleData()

    Row {
        // This scope will recompose when currentValue changes
        Text("Data: $currentValue")
    }
}

// TODO: operator assignment for local delegated properties is currently not supported
// https://github.com/JetBrains/kotlin/blob/11f3c4b03f40460160c1f23b634941a867fd817b/compiler/backend/src/org/jetbrains/kotlin/codegen/StackValue.java#L2268
@Suppress("ReplaceWithOperatorAssignment")
@Sampled
@Composable
fun DelegatedStateSample() {
    var count by remember { mutableStateOf(0) }

    Text(text = "You clicked $count times")
    Button(onClick = { count = count + 1 }) {
        Text("Click me")
    }
}

private class Subscription {
    fun unsubscribe() {}
}

@Suppress("UNUSED_PARAMETER")
private object UserAPI {
    fun subscribeToUser(userId: Int, user: (User) -> Unit): Subscription {
        return Subscription()
    }
}

private val elements = listOf<Element>()

private class Element(val id: Int)

@Suppress("UNUSED_PARAMETER")
@Composable
private fun ListItem(item: Any, selected: Boolean) {}

private const val parentId = 0

@Suppress("CanBeVal", "unused")
@Sampled
@Composable
fun DerivedStateSample() {
    @Composable fun CountDisplay(count: State<Int>) {
        Text("Count: ${count.value}")
    }

    @Composable fun Example() {
        var a by remember { mutableStateOf(0) }
        var b by remember { mutableStateOf(0) }
        val sum = remember { derivedStateOf { a + b } }
        // Changing either a or b will cause CountDisplay to recompose but not trigger Example
        // to recompose.
        CountDisplay(sum)
    }
}