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

@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "LocalVariableName")

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

@Sampled
fun createCompositionLocal() {
    val ActiveUser = compositionLocalOf<User> { error("No active user found!") }
}

@Sampled
fun compositionLocalProvider() {
    @Composable
    fun App(user: User) {
        CompositionLocalProvider(ActiveUser provides user) {
            SomeScreen()
        }
    }
}

@Sampled
fun someScreenSample() {
    @Composable
    fun SomeScreen() {
        UserPhoto()
    }
}

@Sampled
fun consumeCompositionLocal() {
    @Composable
    fun UserPhoto() {
        val user = ActiveUser.current
        ProfileIcon(src = user.profilePhotoUrl)
    }
}

@Suppress("CompositionLocalNaming")
private val ActiveUser = compositionLocalOf<User> { error("No active user found!") }

@Composable private fun SomeScreen() {}

@Composable private fun UserPhoto() {}
