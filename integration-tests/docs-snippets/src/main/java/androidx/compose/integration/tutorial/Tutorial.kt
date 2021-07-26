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

// Ignore lint warnings in documentation snippets
@file:Suppress("unused", "UNUSED_PARAMETER")

package androidx.compose.integration.tutorial

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.integration.tutorial.Lesson2_Layouts.Snippet4.MessageCard
import androidx.compose.integration.tutorial.Lesson2_Layouts.Snippet1.Message
import androidx.compose.integration.tutorial.Lesson4_ListsAnimations.Snippet1.Conversation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/tutorial
 *
 * No action required if it's modified.
 */

private object Lesson1_ComposableFunctions {
    object Snippet1 {
        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    Text("Hello world!")
                }
            }
        }
    }
    object Snippet2 {
        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MessageCard("Android")
                }
            }
        }

        @Composable
        fun MessageCard(name: String) {
            Text(text = "Hello $name!")
        }
    }

    object Snippet3 {
        @Composable
        fun MessageCard(name: String) {
            Text(text = "Hello $name!")
        }

        @Preview
        @Composable
        fun PreviewMessageCard() {
            MessageCard("Android")
        }
    }
}

private object Lesson2_Layouts {
    object Snippet1 {
        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MessageCard(Message("Android", "Jetpack Compose"))
                }
            }
        }

        data class Message(val author: String, val body: String)

        @Composable
        fun MessageCard(msg: Message) {
            Text(text = msg.author)
            Text(text = msg.body)
        }

        @Preview
        @Composable
        fun PreviewMessageCard() {
            MessageCard(
                msg = Message("Colleague", "Hey, take a look at Jetpack Compose, it's great!")
            )
        }
    }

    object Snippet2 {
        @Composable
        fun MessageCard(msg: Message) {
            Column {
                Text(text = msg.author)
                Text(text = msg.body)
            }
        }
    }

    object Snippet3 {
        @Composable
        fun MessageCard(msg: Message) {
            Row {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = "Contact profile picture",
                )

                Column {
                    Text(text = msg.author)
                    Text(text = msg.body)
                }
            }
        }
    }

    object Snippet4 {
        @Composable
        fun MessageCard(msg: Message) {
            // Add padding around our message
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        // Set image size to 40 dp
                        .size(40.dp)
                        // Clip image to be shaped as a circle
                        .clip(CircleShape)
                )

                // Add a horizontal space between the image and the column
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(text = msg.author)
                    // Add a vertical space between the author and message texts
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg.body)
                }
            }
        }
    }
}

private object Lesson3_MaterialDesign {
    object Snippet1 {
        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    ComposeTutorialTheme {
                        MessageCard(Message("Android", "Jetpack Compose"))
                    }
                }
            }
        }

        @Preview
        @Composable
        fun PreviewMessageCard() {
            ComposeTutorialTheme {
                MessageCard(
                    msg = Message("Colleague", "Hey, take a look at Jetpack Compose, it's great!")
                )
            }
        }
    }

    object Snippet2 {
        @Composable
        fun MessageCard(msg: Message) {
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = msg.author,
                        color = MaterialTheme.colors.secondaryVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg.body)
                }
            }
        }
    }

    object Snippet3 {
        @Composable
        fun MessageCard(msg: Message) {
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = msg.author,
                        color = MaterialTheme.colors.secondaryVariant,
                        style = MaterialTheme.typography.subtitle2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }

    object Snippet4 {
        @Composable
        fun MessageCard(msg: Message) {
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.secondary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = msg.author,
                        color = MaterialTheme.colors.secondaryVariant,
                        style = MaterialTheme.typography.subtitle2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(shape = MaterialTheme.shapes.medium, elevation = 1.dp) {
                        Text(
                            text = msg.body,
                            modifier = Modifier.padding(all = 4.dp),
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }

        object Snippet5 {
            @Preview(name = "Light Mode")
            @Preview(
                uiMode = Configuration.UI_MODE_NIGHT_YES,
                showBackground = true,
                name = "Dark Mode"
            )
            @Composable
            fun PreviewMessageCard() {
                ComposeTutorialTheme {
                    MessageCard(
                        msg = Message("Colleague", "Take a look at Jetpack Compose, it's great!")
                    )
                }
            }
        }
    }
}

private object Lesson4_ListsAnimations {
    object Snippet1 {
        // import androidx.compose.foundation.lazy.items

        @Composable
        fun Conversation(messages: List<Message>) {
            LazyColumn {
                items(messages) { message ->
                    MessageCard(message)
                }
            }
        }

        @Preview
        @Composable
        fun PreviewConversation() {
            ComposeTutorialTheme {
                val messages = List(15) {
                    Message("Colleague", "Hey, take a look at Jetpack Compose, it's great!")
                }

                Conversation(messages)
            }
        }
    }

    object Snippet2 {
        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    ComposeTutorialTheme {
                        val messages = List(15) {
                            Message(
                                "Colleague",
                                "Hey, take a look at Jetpack Compose, it's great!\n" +
                                    "It's the Android's modern toolkit for building native UI." +
                                    "It simplifies and accelerates UI development on Android." +
                                    "Quickly bring your app to life with less code, powerful " +
                                    "tools, and intuitive Kotlin APIs"
                            )
                        }

                        Conversation(messages)
                    }
                }
            }
        }

        @Composable
        fun MessageCard(msg: Message) {
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.secondaryVariant, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // We keep track if the message is expanded or not in this
                // variable
                var isExpanded by remember { mutableStateOf(false) }

                // We toggle the isExpanded variable when we click on this Column
                Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
                    Text(
                        text = msg.author,
                        color = MaterialTheme.colors.secondaryVariant,
                        style = MaterialTheme.typography.subtitle2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        elevation = 1.dp,
                    ) {
                        Text(
                            text = msg.body,
                            modifier = Modifier.padding(all = 4.dp),
                            // If the message is expanded, we display all its content
                            // otherwise we only display the first line
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }
    }

    object Snippet4 {
        @Composable
        fun MessageCard(msg: Message) {
            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = painterResource(R.drawable.profile_picture),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.secondaryVariant, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // We keep track if the message is expanded or not in this
                // variable
                var isExpanded by remember { mutableStateOf(false) }
                // surfaceColor will be updated gradually from one color to the other
                val surfaceColor: Color by animateColorAsState(
                    if (isExpanded) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                )

                // We toggle the isExpanded variable when we click on this Column
                Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
                    Text(
                        text = msg.author,
                        color = MaterialTheme.colors.secondaryVariant,
                        style = MaterialTheme.typography.subtitle2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        elevation = 1.dp,
                        // surfaceColor color will be changing gradually from primary to surface
                        color = surfaceColor,
                        // animateContentSize will change the Surface size gradually
                        modifier = Modifier.animateContentSize().padding(1.dp)
                    ) {
                        Text(
                            text = msg.body,
                            modifier = Modifier.padding(all = 4.dp),
                            // If the message is expanded, we display all its content
                            // otherwise we only display the first line
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
            }
        }
    }
}

// ========================
// Fakes below
// ========================

@Composable
private fun ComposeTutorialTheme(content: @Composable () -> Unit) = MaterialTheme(content = content)

private object R {
    object drawable {
        const val profile_picture = 1
    }
}

@Repeatable
@Retention(AnnotationRetention.SOURCE)
private annotation class Preview(
    val name: String = "",
    val uiMode: Int = 0,
    val showBackground: Boolean = true
)
