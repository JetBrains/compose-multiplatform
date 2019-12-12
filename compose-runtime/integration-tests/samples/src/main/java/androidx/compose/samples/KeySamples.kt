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

@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.key

@Sampled
@Composable
fun locallyUniqueKeys() {
    for (user in users) {
        key(user.id) { UserPreview(user = user) }
    }

    for (user in users.filter { isAdmin }) {
        key(user.id) { Friend(friend = user) }
    }
}

@Sampled
@Composable
fun notAlwaysUniqueKeys() {
    for ((child, parent) in relationships) {
        key(parent.id) {
            User(user = child)
            User(user = parent)
        }
    }
}

@Sampled
@Composable
fun moreCorrectUniqueKeys() {
    for ((child, parent) in relationships) {
        key(parent.id to child.id) {
            User(user = child)
            User(user = parent)
        }
    }
}

@Composable private fun User(user: User) {}

@Composable private fun UserPreview(user: User) {}

@Composable private fun Friend(friend: User) {}

private const val isAdmin = true

private val users = listOf<User>()
private val relationships = mapOf<User, User>()
private val child = User()
