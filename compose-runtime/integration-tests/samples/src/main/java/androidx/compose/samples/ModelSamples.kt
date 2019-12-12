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

import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.remember
import androidx.ui.core.TextField
import androidx.ui.material.Button

@Composable
fun modelSample() {
    @Model
    class LoginState(var username: String, var password: String) {
        fun login() = Api.login(username, password)
    }

    @Composable
    fun LoginScreen() {
        val model = remember { LoginState("user", "pass") }

        TextField(
            value = model.username,
            onValueChange = { model.username = it }
        )
        TextField(
            value = model.password,
            onValueChange = { model.password = it }
        )
        Button(text = "Login", onClick = { model.login() })
    }
}
