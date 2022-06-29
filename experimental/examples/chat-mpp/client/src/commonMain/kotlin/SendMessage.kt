/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SendMessage(sendMessage: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    TextField(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(10.dp),
        value = inputText,
        placeholder = {
            Text("type message here")
        },
        onValueChange = {
            inputText = it
        },
        trailingIcon = {
            if (inputText.isNotEmpty()) {
                Icon(
                    modifier = Modifier
                        .clickable {
                            sendMessage(inputText)
                            inputText = ""
                        }
                        .padding(10.dp),
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    )
}