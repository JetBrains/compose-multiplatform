/*
 * Copyright 2020 The Android expanded Source Project
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

package androidx.compose.material.demos

import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DropdownMenuVariationsDemo() {
    Column(
        modifier = Modifier.fillMaxHeight().width(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.requiredHeight(10.dp))

        Text("Default Menu (not focusable)", fontSize = 20.sp)
        DropdownMenuInstance()

        Spacer(Modifier.requiredHeight(10.dp))

        Text("Focusable menu", fontSize = 20.sp)
        DropdownMenuInstance(PopupProperties(focusable = true))

        Spacer(Modifier.requiredHeight(10.dp))

        Text(
            "Focusable Menu which propagates clicks",
            fontSize = 20.sp
        )
        DropdownMenuInstance(
            PopupProperties(
                focusable = true,
                updateAndroidWindowManagerFlags = { flags ->
                    flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                }
            )
        )
    }
}

@Composable
fun DropdownMenuInstance(popupProperties: PopupProperties = PopupProperties()) {
    val options = listOf(
        "Refresh",
        "Settings",
        "Send Feedback",
        "Help",
        "Signout"
    )

    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text("SHOW MENU")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = popupProperties,
        ) {
            options.forEach {
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text(it)
                }
            }
        }
    }
}