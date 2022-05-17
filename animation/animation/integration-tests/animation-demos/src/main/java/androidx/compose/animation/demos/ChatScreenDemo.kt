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

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun ChatScreen() {
    Surface(color = Color(0xff21181d)) {
        Column {
            val messages = remember { mutableStateListOf<Message>() }
            Button(
                onClick = {
                    messages.add(
                        when (messages.size) {
                            0 -> Message("Hey!", true)
                            1 -> Message("Why does a duck have tail feathers?", true)
                            2 -> Message("I don't know", false)
                            3 -> Message("Why does it?", false)
                            4 -> Message("To cover its butt quack", true)
                            5 -> Message("\uD83D\uDE02", true)
                            6 -> Message("...", false)
                            else -> Message("Haha very funny \uD83D\uDE44", false)
                        }
                    )
                }
            ) {
                Text("Next message")
            }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                for (i in messages.indices) {
                    if (messages[i].isMyMessage) {
                        MyChatEntry(
                            text = messages[i].text,
                            hasMessageAbove = hasMessageAbove(messages, i),
                            hasMessageBelow = hasMessageBelow(messages, i)
                        )
                    } else {
                        TheirChatEntry(
                            text = messages[i].text,
                            hasMessageAbove = hasMessageAbove(messages, i),
                            hasMessageBelow = hasMessageBelow(messages, i)
                        )
                    }
                }
            }
        }
    }
}

private data class Message(val text: String, val isMyMessage: Boolean)

private fun hasMessageAbove(messages: List<Message>, index: Int): Boolean {
    val previous = index - 1
    if (previous < 0) return false
    return messages[index].isMyMessage == messages[previous].isMyMessage
}

private fun hasMessageBelow(messages: List<Message>, index: Int): Boolean {
    val next = index + 1
    if (next >= messages.size) return false
    return messages[index].isMyMessage == messages[next].isMyMessage
}

private val myMessageBgColor = Color(0xfffcd5e2)
private val myMessageTextColor = Color(0xff4d353d)
private val theirMessageBgColor = Color(0xff3a272d)
private val theirMessageTextColor = Color(0xffa7979c)
private const val roundCornerSize = 16f

@Composable
private fun animateCorner(hasSharpCorner: Boolean): MutableState<Float> {
    val state = remember { mutableStateOf(roundCornerSize) }
    LaunchedEffect(hasSharpCorner) {
        animate(
            initialValue = state.value,
            targetValue = if (hasSharpCorner) 2f else roundCornerSize,
            animationSpec = spring(stiffness = 50f, dampingRatio = 0.6f)
        ) { animationValue, _ -> state.value = animationValue }
    }
    return state
}

@Composable
private fun ColumnScope.MyChatEntry(
    text: String,
    hasMessageAbove: Boolean,
    hasMessageBelow: Boolean
) {
    val topCorner by animateCorner(hasMessageAbove)
    val bottomCorner by animateCorner(hasMessageBelow)
    ChatEntry(
        text = text,
        textColor = myMessageTextColor,
        backgroundColor = myMessageBgColor,
        shape = RoundedCornerShape(
            topStart = roundCornerSize.dp,
            topEnd = topCorner.dp,
            bottomStart = roundCornerSize.dp,
            bottomEnd = bottomCorner.dp
        ),
        alignment = Alignment.End
    )
}

@Composable
private fun ColumnScope.TheirChatEntry(
    text: String,
    hasMessageAbove: Boolean,
    hasMessageBelow: Boolean
) {
    val topCorner by animateCorner(hasMessageAbove)
    val bottomCorner by animateCorner(hasMessageBelow)
    ChatEntry(
        text = text,
        textColor = theirMessageTextColor,
        backgroundColor = theirMessageBgColor,
        shape = RoundedCornerShape(
            topStart = topCorner.dp,
            topEnd = roundCornerSize.dp,
            bottomStart = bottomCorner.dp,
            bottomEnd = roundCornerSize.dp
        ),
        alignment = Alignment.Start
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ColumnScope.ChatEntry(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    shape: Shape,
    alignment: Alignment.Horizontal
) {
    AnimatedVisibility(
        visibleState = remember { MutableTransitionState(false).apply { targetState = true } },
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(stiffness = 50f, dampingRatio = 0.6f)
        ),
        modifier = Modifier.align(alignment)
    ) {
        Card(
            backgroundColor = backgroundColor,
            shape = shape,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                color = textColor,
                text = text
            )
        }
    }
}
