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
import androidx.compose.Pivotal
import androidx.compose.onActive
import androidx.compose.onCommit
import androidx.compose.state
import androidx.ui.core.Text

@Sampled
@Composable
fun incorrectUserOrdering() {
    @Composable
    fun UserList(userIds: List<Int>) {
        for (id in userIds) {
            UserRow(userId = id)
        }
    }

    @Composable
    fun UserRow(userId: Int) {
        var user by state<User?> { null }
        onActive {
            val dispose = Api.getUserAsync(userId) { user = it }
            onDispose(dispose)
        }

        if (user == null) {
            LoadingIndicator()
            return
        }
        ProfileIcon(src = user!!.profilePhotoUrl)
        Text(text = user!!.name)
    }
}

@Sampled
@Composable
fun expensiveApiCalls() {
    @Composable
    fun UserRow(userId: Int) {
        var user by state<User?> { null }
        onCommit(userId) {
            val dispose = Api.getUserAsync(userId) { user = it }
            onDispose(dispose)
        }

        if (user == null) {
            LoadingIndicator()
            return
        }
        ProfileIcon(src = user!!.profilePhotoUrl)
        Text(text = user!!.name)
    }
}

@Sampled
@Composable
fun pivotalUsage() {
    @Composable
    fun UserRow(@Pivotal userId: Int) {
        var user by state<User?> { null }
        onActive {
            val dispose = Api.getUserAsync(userId) { user = it }
            onDispose(dispose)
        }

        if (user == null) {
            LoadingIndicator()
            return
        }
        ProfileIcon(src = user!!.profilePhotoUrl)
        Text(text = user!!.name)
    }
}

@Composable private fun LoadingIndicator() {}

@Composable private fun UserRow(userId: Int) {}
