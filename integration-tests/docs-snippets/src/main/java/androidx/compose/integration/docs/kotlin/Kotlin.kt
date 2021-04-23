// ktlint-disable indent https://github.com/pinterest/ktlint/issues/967
/*
 * Copyright 2020 The Android Open Source Project
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
@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE")

package androidx.compose.integration.docs.kotlin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * This file lets DevRel track changes to snippets present in
 * https://developer.android.com/jetpack/compose/kotlin
 *
 * No action required if it's modified.
 */

@Composable private fun KotlinSnippet1() {
    Text(text = "Hello, Android!")
}

@Composable private fun KotlinSnippet2() {
    Text(
        text = "Hello, Android!",
        color = Color.Unspecified,
        fontSize = TextUnit.Unspecified,
        letterSpacing = TextUnit.Unspecified,
        overflow = TextOverflow.Clip
    )
}

@Composable private fun KotlinSnippet3() {
    Button(
        // ...
        onClick = {
            // do something
            // do something else
        }
    ) { /* ... */ }
}

@Composable private fun KotlinSnippet4() {
    Column(
        modifier = Modifier.padding(16.dp),
        content = {
            Text("Some text")
            Text("Some more text")
            Text("Last text")
        }
    )
}

@Composable private fun KotlinSnippet5() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Some text")
        Text("Some more text")
        Text("Last text")
    }
}

@Composable private fun KotlinSnippet6() {
    Column {
        Text("Some text")
        Text("Some more text")
        Text("Last text")
    }
}

@Composable private fun KotlinSnippet7() {
    Row {
        Text(
            text = "Hello world",
            // This Text is inside a RowScope so it has access to
            // Alignment.CenterVertically but not to
            // Alignment.CenterHorizontally, which would be available
            // in a ColumnScope.
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable private fun KotlinSnippet8() {
    Box(
        modifier = Modifier.drawBehind {
            // This method accepts a lambda of type DrawScope.() -> Unit
            // therefore in this lambda we can access properties and functions
            // available from DrawScope, such as the `drawRectangle` function.
            drawRect(/* ... */)
        }
    )
}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VALUE")
@Composable private fun KotlinSnippet9() {
    var showDialog by remember { mutableStateOf(false) }

    // Updating the var automatically triggers a state change
    showDialog = true
}

// TODO: uncomment when constraint layout for compose releases, and add that dependency
/*
@Suppress("Deprecation")
@Composable private fun KotlinSnippet10() {
    ConstraintLayout {

        val (image, title, subtitle) = createRefs()

        // The `createRefs` function returns a data object;
        // the first three components are extracted into the
        // image, title, and subtitle variables.

        // ...
    }
}*/

@Composable private fun KotlinSnippet11() {
    // Create a CoroutineScope that follows this composable's lifecycle
    val composableScope = rememberCoroutineScope()
    Button( // ...
        onClick = {
            // Create a new coroutine that scrolls to the top of the list
            // and call the ViewModel to load data
            composableScope.launch {
                scrollState.animateScrollTo(0) // This is a suspend function
                viewModel.loadData()
            }
        }
    ) { /* ... */ }
}

private object KotlinSnippet12 {
    @Composable
    fun MessageList(messages: List<Message>) {
        LazyColumn {
            // Add a single item as a header
            item {
                Text("Message List")
            }

            // Add list of messages
            items(messages) { message ->
                Message(message)
            }
        }
    }
}

@Composable private fun KotlinSnippet13() {
    Canvas(Modifier.size(120.dp)) {
        // Draw grey background, drawRect function is provided by the receiver
        drawRect(color = Color.Gray)

        // Inset content by 10 pixels on the left/right sides
        // and 12 by the top/bottom
        inset(10.0f, 12.0f) {
            val quadrantSize = size / 2.0f

            // Draw a rectangle within the inset bounds
            drawRect(
                size = quadrantSize,
                color = Color.Red
            )

            rotate(45.0f) {
                drawRect(size = quadrantSize, color = Color.Blue)
            }
        }
    }
}

@Composable private fun KotlinSnippet14() {
    // Create a CoroutineScope that follows this composable's lifecycle
    val composableScope = rememberCoroutineScope()
    Button(
        // ...
        onClick = {
            // Create a new coroutine that scrolls to the top of the list
            // and call the ViewModel to load data
            composableScope.launch {
                scrollState.animateScrollTo(0) // This is a suspend function
                viewModel.loadData()
            }
        }
    ) { /* ... */ }
}

@Composable private fun KotlinSnippet15() {
    // Create a CoroutineScope that follows this composable's lifecycle
    val composableScope = rememberCoroutineScope()
    Button( // ...
        onClick = {
            // Scroll to the top and load data in parallel by creating a new
            // coroutine per independent work to do
            composableScope.launch {
                scrollState.animateScrollTo(0)
            }
            composableScope.launch {
                viewModel.loadData()
            }
        }
    ) { /* ... */ }
}

private object KotlinSnippet16 {
    @Composable
    fun MoveBoxWhereTapped() {
        // Creates an `Animatable` to animate Offset and `remember` it.
        val animatedOffset = remember {
            Animatable(Offset(0f, 0f), Offset.VectorConverter)
        }

        Box(
            // The pointerInput modifier takes a suspend block of code
            Modifier.fillMaxSize().pointerInput(Unit) {
                // Create a new CoroutineScope to be able to create new
                // coroutines inside a suspend function
                coroutineScope {
                    while (true) {
                        // Wait for the user to tap on the screen
                        val offset = awaitPointerEventScope {
                            awaitFirstDown().position
                        }
                        // Launch a new coroutine to asynchronously animate to where
                        // the user tapped on the screen
                        launch {
                            // Animate to the pressed position
                            animatedOffset.animateTo(offset)
                        }
                    }
                }
            }
        ) {
            Text("Tap anywhere", Modifier.align(Alignment.Center))
            Box(
                Modifier
                    .offset {
                        // Use the animated offset as the offset of this Box
                        IntOffset(
                            animatedOffset.value.x.roundToInt(),
                            animatedOffset.value.y.roundToInt()
                        )
                    }
                    .size(40.dp)
                    .background(Color(0xff3c1361), CircleShape)
            )
        }
    }
}

/*
Fakes needed for snippets to build:
 */

@Suppress("ClassName")
private object R {
    object string {
        const val profile = 0
        const val friends_list = 1
    }
}

private fun drawRect() {}

private val scrollState = ScrollState(0)
private class MyViewModel { fun loadData() {} }
private val viewModel = MyViewModel()
private class Message

@Composable private fun Message(message: Message) { }