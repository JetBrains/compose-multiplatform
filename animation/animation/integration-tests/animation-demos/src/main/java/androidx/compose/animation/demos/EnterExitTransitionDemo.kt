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

package androidx.compose.animation.demos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EnterExitTransitionDemo() {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        val oppositeAlignment = remember { mutableStateOf(true) }

        var alignment by remember { mutableStateOf(TopStart) }
        var visible by remember { mutableStateOf(true) }
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(0) }
        Column(Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier.align(CenterHorizontally),
                onClick = {
                    alignment = TopCenter
                    visible = !visible
                }
            ) {
                Text("Top")
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.fillMaxHeight().wrapContentWidth()) {
                    Button(
                        modifier = Modifier.align(TopEnd),
                        onClick = {
                            alignment = TopStart
                            visible = !visible
                        }
                    ) {
                        Text("Top\nStart")
                    }
                    Button(
                        modifier = Modifier.align(CenterEnd),
                        onClick = {
                            alignment = CenterStart
                            visible = !visible
                        }
                    ) {
                        Text("Start")
                    }
                    Button(
                        modifier = Modifier.align(BottomEnd),
                        onClick = {
                            alignment = BottomStart
                            visible = !visible
                        }
                    ) {
                        Text("Bottom\nStart")
                    }
                }
                CenterMenu(selectedOption, oppositeAlignment.value, alignment, visible)
                Box(Modifier.fillMaxHeight().wrapContentWidth()) {
                    Button(
                        modifier = Modifier.align(TopStart),
                        onClick = {
                            alignment = TopEnd
                            visible = !visible
                        }
                    ) {
                        Text("Top\nEnd")
                    }
                    Button(
                        modifier = Modifier.align(CenterStart),
                        onClick = {
                            alignment = CenterEnd
                            visible = !visible
                        }
                    ) {
                        Text("End")
                    }
                    Button(
                        modifier = Modifier.align(BottomEnd),
                        onClick = {
                            alignment = BottomEnd
                            visible = !visible
                        }
                    ) {
                        Text("Bottom\nEnd")
                    }
                }
            }
            Button(
                modifier = Modifier.align(CenterHorizontally),
                onClick = {
                    alignment = BottomCenter
                    visible = !visible
                }
            ) {
                Text("Bottom")
            }

            AlignmentOption(oppositeAlignment)
            FadeOptions(selectedOption, onOptionSelected)
        }
    }
}

@Composable
fun AlignmentOption(state: MutableState<Boolean>) {
    Row(
        Modifier.selectable(selected = state.value, onClick = { state.value = !state.value })
            .padding(10.dp)
    ) {
        Checkbox(state.value, { state.value = it })
        Text("Animate opposite to container alignment")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CenterMenu(
    selectedOption: Int,
    oppositeDirection: Boolean,
    alignment: Alignment,
    visible: Boolean
) {
    Box(with(RowScope) { Modifier.fillMaxHeight().weight(1f) }) {

        val animationAlignment = if (oppositeDirection) opposite(alignment) else alignment
        val enter = when (animationAlignment) {
            TopCenter -> expandVertically(expandFrom = Top)
            BottomCenter -> expandVertically(expandFrom = Bottom)
            CenterStart -> expandHorizontally(expandFrom = Start)
            CenterEnd -> expandHorizontally(expandFrom = End)
            else -> expandIn(animationAlignment)
        }.run {
            if (selectedOption >= 1) {
                this + fadeIn()
            } else {
                this
            }
        }

        val exit = when (animationAlignment) {
            TopCenter -> shrinkVertically(shrinkTowards = Top)
            BottomCenter -> shrinkVertically(shrinkTowards = Bottom)
            CenterStart -> shrinkHorizontally(shrinkTowards = Start)
            CenterEnd -> shrinkHorizontally(shrinkTowards = End)
            else -> shrinkOut(animationAlignment)
        }.run {
            if (selectedOption >= 2) {
                this + fadeOut()
            } else {
                this
            }
        }
        AnimatedVisibility(
            visible,
            Modifier.align(alignment),
            enter = enter,
            exit = exit
        ) {
            val menuText = remember {
                mutableListOf<String>().apply {
                    for (i in 0..15) {
                        add("Menu Item $i")
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().background(Color(0xFFd8c7ff))) {
                items(menuText) {
                    Text(it, Modifier.padding(5.dp))
                }
            }
        }
    }
}

@Composable
fun FadeOptions(selectedOption: Int, onOptionSelected: (Int) -> Unit) {
    Column {
        Text(
            text = "Combine with:",
            modifier = Modifier.padding(start = 16.dp)
        )
        val radioOptions =
            listOf("No Fade", "Fade In", "Fade Out", "Fade In & Fade out")
        radioOptions.forEachIndexed { i, text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .preferredHeight(30.dp)
                    .selectable(
                        selected = (i == selectedOption),
                        onClick = { onOptionSelected(i) }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (i == selectedOption),
                    onClick = { onOptionSelected(i) }
                )
                Text(
                    text = text,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

fun opposite(alignment: Alignment): Alignment =
    when (alignment) {
        TopStart -> BottomEnd
        CenterStart -> CenterEnd
        BottomStart -> TopEnd
        TopEnd -> BottomStart
        CenterEnd -> CenterStart
        BottomEnd -> TopStart
        TopCenter -> BottomCenter
        BottomCenter -> TopCenter
        else -> alignment
    }