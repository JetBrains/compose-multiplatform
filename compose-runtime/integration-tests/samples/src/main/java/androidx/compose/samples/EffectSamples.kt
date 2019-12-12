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

package androidx.compose.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.key
import androidx.compose.onCommit
import androidx.compose.state
import androidx.compose.stateFor
import androidx.ui.core.Text
import androidx.ui.material.Button

@Suppress("unused")
@Sampled
@Composable
fun observeUserSample() {
    @Composable
    fun observeUser(userId: Int): User? {
        val user = stateFor<User?>(userId) { null }
        onCommit(userId) {
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
fun twoInputsKeySample() {
    for (element in elements) {
        val selected by key(element.id, parentId) { state { false } }
        ListItem(item = element, selected = selected)
    }
}

@Sampled
@Composable
fun SimpleStateSample() {
    val count = state { 0 }

    Text(text = "You clicked ${count.value} times")
    Button(text = "Click me", onClick = { count.value++ })
}

@Sampled
@Composable
fun DestructuredStateSample() {
    val (count, setCount) = state { 0 }

    Text(text = "You clicked $count times")
    Button(text = "Click me", onClick = { setCount(count + 1) })
}

// TODO: operator assignment for local delegated properties is currently not supported
// https://github.com/JetBrains/kotlin/blob/11f3c4b03f40460160c1f23b634941a867fd817b/compiler/backend/src/org/jetbrains/kotlin/codegen/StackValue.java#L2268
@Suppress("ReplaceWithOperatorAssignment")
@Sampled
@Composable
fun DelegatedStateSample() {
    var count by state { 0 }

    Text(text = "You clicked $count times")
    Button(text = "Click me", onClick = { count = count + 1 })
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
