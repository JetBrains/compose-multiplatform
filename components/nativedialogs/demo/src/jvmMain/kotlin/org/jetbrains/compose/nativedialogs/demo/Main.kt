package org.jetbrains.compose.nativedialogs.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.singleWindowApplication
import org.jetbrains.compose.nativedialogs.pickFolder

fun main() {
    singleWindowApplication(
        title = "Native Dialogs demo"
    ) {
        var openedFile by remember { mutableStateOf("") }

        Column {
            Text(openedFile)

            Button(onClick = {
                pickFolder(System.getProperty("user.home"))?.also {
                    openedFile = it
                }
            }) {
                Text("File")
            }
        }
    }
}