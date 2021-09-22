/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.modifier.modifierLocalOf

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun ModifierLocalParentChildCommunicationWithinLayoutNodeSample() {

    // Define the type of data.
    val ModifierLocalMessage = modifierLocalOf { "Unknown" }

    Box(
        Modifier
            // Provide an instance associated with the data type.
            .modifierLocalProvider(ModifierLocalMessage) { "World" }
            .composed {
                var message by remember { mutableStateOf("") }
                Modifier
                    // Use the data type to read the message.
                    .modifierLocalConsumer { message = ModifierLocalMessage.current }
                    .clickable { println("Hello $message") }
            }

    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun ModifierLocalChildParentCommunicationWithinLayoutNodeSample() {

    class Sender(val onMessageReceived: (String) -> Unit) {
        fun sendMessage(message: String) {
            onMessageReceived(message)
        }
    }

    // Define the type of data.
    val ModifierLocalSender = modifierLocalOf<Sender> { error("No sender provided by parent.") }

    Box(
        Modifier
            // Provide an instance associated with the sender type.
            .modifierLocalProvider(ModifierLocalSender) {
                Sender { println("Message Received: $it") }
            }
            .composed {
                var sender by remember { mutableStateOf<Sender?>(null) }
                Modifier
                    // Use the sender type to fetch an instance.
                    .modifierLocalConsumer { sender = ModifierLocalSender.current }
                    // Use this instance to send a message to the parent.
                    .clickable { sender?.sendMessage("Hello World") }
            }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun ModifierLocalParentChildCommunicationInterLayoutNodeSample() {

    // Define the type of data.
    val ModifierLocalMessage = modifierLocalOf { "Unknown" }

    Box(
        // Provide an instance associated with the data type.
        Modifier.modifierLocalProvider(ModifierLocalMessage) { "World" }
    ) {
        var message by remember { mutableStateOf("") }
        Box(
            Modifier
                // Use the data type to read the message.
                .modifierLocalConsumer { message = ModifierLocalMessage.current }
                .clickable { println("Hello $message") }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Sampled
@Composable
fun ModifierLocalChildParentCommunicationInterLayoutNodeSample() {

    class Sender(val onMessageReceived: (String) -> Unit) {
        fun sendMessage(message: String) {
            onMessageReceived(message)
        }
    }

    // Define the type of data.
    val ModifierLocalSender = modifierLocalOf<Sender> { error("No sender provided by parent.") }

    Box(
        Modifier
            // Provide an instance associated with the sender type.
            .modifierLocalProvider(ModifierLocalSender) {
                Sender { println("Message Received: $it") }
            }
    ) {
        var sender by remember { mutableStateOf<Sender?>(null) }
        Box(
            Modifier
                // Use the sender type to fetch an instance.
                .modifierLocalConsumer { sender = ModifierLocalSender.current }
                // Use this instance to send a message to the parent.
                .clickable { sender?.sendMessage("Hello World") }
        )
    }
}