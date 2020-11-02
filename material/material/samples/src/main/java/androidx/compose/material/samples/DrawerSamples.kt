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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.material.BottomDrawerLayout
import androidx.compose.material.BottomDrawerValue
import androidx.compose.material.Button
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawerLayout
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomDrawerState
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun ModalDrawerSample() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    ModalDrawerLayout(
        drawerState = drawerState,
        drawerContent = {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                onClick = { drawerState.close() },
                content = { Text("Close Drawer") }
            )
        },
        bodyContent = {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = if (drawerState.isClosed) ">>> Swipe >>>" else "<<< Swipe <<<")
                Spacer(Modifier.preferredHeight(20.dp))
                Button(onClick = { drawerState.open() }) {
                    Text("Click to open")
                }
            }
        }
    )
}

@Sampled
@Composable
fun BottomDrawerSample() {
    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
    BottomDrawerLayout(
        drawerState = drawerState,
        drawerContent = {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                onClick = { drawerState.close() },
                content = { Text("Close Drawer") }
            )
        },
        bodyContent = {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = if (drawerState.isClosed) "▲▲▲ Pull up ▲▲▲" else "▼▼▼ Drag down ▼▼▼")
                Spacer(Modifier.preferredHeight(20.dp))
                Button(onClick = { drawerState.open() }) {
                    Text("Click to open")
                }
            }
        }
    )
}