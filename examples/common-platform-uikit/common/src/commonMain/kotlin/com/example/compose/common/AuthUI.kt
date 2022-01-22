/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example.compose.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    var email: String by remember { mutableStateOf("") }
    var password: String by remember { mutableStateOf("") }
    var authInProgress: Boolean by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.width(300).align(Alignment.Center)) {
            Text(text = "Вход")
            TextField(label = "E-mail", value = email) { email = it }
            TextField(label = "Password", value = password) { password = it }
            if (authInProgress) {
                ProgressBar()
            } else {
                Button(text = "Войти", onClick = {
                    authInProgress = true

                    GlobalScope.launch(Dispatchers.Main) {
                        delay(2000)
                        authInProgress = false
                    }
                })
            }
        }
    }
}
