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

package androidx.compose.ui.demos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun PopupDemo() {
    val exampleIndex = remember { mutableStateOf(0) }
    val totalExamples = 8

    Column {
        Row(
            Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClickableTextWithBackground(
                text = "Prev",
                color = Color.Cyan,
                onClick = {
                    if (exampleIndex.value == 0) {
                        exampleIndex.value = totalExamples
                    }

                    exampleIndex.value = (exampleIndex.value - 1) % totalExamples
                },
                padding = 20.dp
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val description: String = {
                    when (exampleIndex.value) {
                        0 -> "Shadow demo"
                        1 -> "Toggle a simple popup"
                        2 -> "Different content for the popup"
                        3 -> "Popup's behavior when the parent's size or position changes"
                        4 -> "Aligning the popup below the parent"
                        5 -> "Aligning the popup inside a parent"
                        6 ->
                            "Insert an email in the popup and then click outside to dismiss"
                        7 ->
                            "[bug] Undesired visual effect caused by" +
                                " having a new size content displayed at the old" +
                                " position, until the new one is calculated"
                        8 ->
                            "The popup is aligning to its parent when the parent is" +
                                " inside a Scroller"
                        9 ->
                            "[bug] The popup is not repositioned " +
                                "when the parent is moved by the keyboard"
                        else -> "Demo description here"
                    }
                }.invoke()

                Text(
                    text = description,
                    textAlign = TextAlign.Center
                )
            }

            ClickableTextWithBackground(
                text = "Next",
                color = Color.Cyan,
                onClick = {
                    exampleIndex.value = (exampleIndex.value + 1) % totalExamples
                },
                padding = 20.dp
            )
        }

        when (exampleIndex.value) {
            0 -> PopupElevation()
            1 -> PopupToggle()
            2 -> PopupWithChangingContent()
            3 -> PopupWithChangingParent()
            4 -> PopupAlignmentDemo()
            5 -> PopupWithEditText()
            6 -> PopupWithChangingSize()
            7 -> PopupInsideScroller()
            8 -> PopupOnKeyboardUp()
        }
    }
}

@Composable
private fun ColumnScope.PopupElevation() {
    var shape by remember { mutableStateOf(RectangleShape) }
    var background by remember { mutableStateOf(Color.Transparent) }
    var contentSize by remember { mutableStateOf(100.dp) }
    var dismissCounter by remember { mutableStateOf(0) }
    var elevation by remember { mutableStateOf(6.dp) }

    // This example utilizes the Card to draw its shadow.
    Column(Modifier.align(Alignment.CenterHorizontally)) {
        Box(Modifier.size(110.dp).background(background)) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { dismissCounter++ }
            ) {
                Card(
                    Modifier.size(contentSize),
                    elevation = elevation,
                    shape = shape
                ) {
                    Text(text = "This is popup!", textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.requiredHeight(20.dp))
        Text("Dismiss clicked: $dismissCounter")
        Spacer(Modifier.requiredHeight(20.dp))
        Row {
            Button(onClick = { elevation -= 1.dp }) {
                Text("-1")
            }
            Text("Elevation: $elevation")
            Button(onClick = { elevation += 1.dp }) {
                Text("+1")
            }
        }
        Spacer(Modifier.requiredHeight(10.dp))
        Button(onClick = { shape = if (shape == CircleShape) RectangleShape else CircleShape }) {
            Text("Toggle shape")
        }
        Spacer(Modifier.requiredHeight(10.dp))
        Spacer(Modifier.requiredHeight(10.dp))
        Button(
            onClick = {
                background =
                    if (background == Color.Transparent) Color.Yellow else Color.Transparent
            }
        ) {
            Text("Toggle container background")
        }
        Spacer(Modifier.requiredHeight(10.dp))
        Row {
            Button(onClick = { contentSize -= 10.dp }) {
                Text("-10.dp")
            }
            Text("Size: $contentSize")
            Button(onClick = { contentSize += 10.dp }) {
                Text("+10.dp")
            }
        }
    }
}

@Composable
private fun ColumnScope.PopupToggle() {
    val showPopup = remember { mutableStateOf(true) }

    Column(Modifier.align(Alignment.CenterHorizontally)) {
        Box(Modifier.size(100.dp)) {
            if (showPopup.value) {
                Popup(alignment = Alignment.Center) {
                    Box(
                        Modifier.size(70.dp).background(Color.Green, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This is a popup!",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        ClickableTextWithBackground(
            text = "Toggle Popup",
            color = Color.Cyan,
            onClick = {
                showPopup.value = !showPopup.value
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupWithChangingContent() {
    Column(Modifier.align(Alignment.CenterHorizontally)) {
        val heightSize = 120.dp
        val widthSize = 160.dp
        val popupContentState = remember { mutableStateOf(0) }
        val totalContentExamples = 2
        val popupCounter = remember { mutableStateOf(0) }

        Box(Modifier.size(widthSize, heightSize).background(Color.Gray)) {
            Popup(Alignment.Center) {
                when (popupContentState.value % totalContentExamples) {
                    0 -> ClickableTextWithBackground(
                        text = "Counter : ${popupCounter.value}",
                        color = Color.Green,
                        onClick = {
                            popupCounter.value += 1
                        }
                    )
                    1 -> Box(
                        Modifier.size(60.dp, 40.dp).background(Color.Blue, CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ClickableTextWithBackground(
            text = "Change content",
            color = Color.Cyan,
            onClick = {
                popupContentState.value += 1
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupWithChangingParent() {
    val containerWidth = 400.dp
    val containerHeight = 200.dp
    val parentAlignment = remember { mutableStateOf(Alignment.TopStart) }
    val parentWidth = remember { mutableStateOf(80.dp) }
    val parentHeight = remember { mutableStateOf(60.dp) }
    val parentSizeChanged = remember { mutableStateOf(false) }

    Column(Modifier.align(Alignment.CenterHorizontally)) {
        Box(
            Modifier.size(containerWidth, containerHeight),
            contentAlignment = parentAlignment.value
        ) {
            Box(
                Modifier.size(parentWidth.value, parentHeight.value)
                    .background(Color.Blue)
            ) {
                Popup(Alignment.BottomCenter) {
                    Text("Popup", modifier = Modifier.background(color = Color.Green))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ClickableTextWithBackground(
            text = "Change parent's position",
            color = Color.Cyan,
            onClick = {
                parentAlignment.value =
                    if (parentAlignment.value == Alignment.TopStart)
                        Alignment.TopEnd
                    else
                        Alignment.TopStart
            }
        )
        Spacer(Modifier.height(10.dp))
        ClickableTextWithBackground(
            text = "Change parent's size",
            color = Color.Cyan,
            onClick = {
                if (parentSizeChanged.value) {
                    parentWidth.value = 80.dp
                    parentHeight.value = 60.dp
                } else {
                    parentWidth.value = 160.dp
                    parentHeight.value = 120.dp
                }
                parentSizeChanged.value = !parentSizeChanged.value
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupAlignmentDemo() {
    Column(Modifier.align(Alignment.CenterHorizontally)) {
        val heightSize = 200.dp
        val widthSize = 400.dp
        val counter = remember { mutableStateOf(0) }
        val popupAlignment = remember { mutableStateOf(Alignment.TopStart) }
        Box(
            modifier = Modifier.size(widthSize, heightSize).background(Color.Red),
            contentAlignment = Alignment.BottomCenter
        ) {
            Popup(popupAlignment.value) {
                ClickableTextWithBackground(
                    text = "Click to change alignment",
                    color = Color.White,
                    onClick = {
                        counter.value += 1
                        when (counter.value % 9) {
                            0 -> popupAlignment.value = Alignment.TopStart
                            1 -> popupAlignment.value = Alignment.TopCenter
                            2 -> popupAlignment.value = Alignment.TopEnd
                            3 -> popupAlignment.value = Alignment.CenterEnd
                            4 -> popupAlignment.value = Alignment.BottomEnd
                            5 -> popupAlignment.value = Alignment.BottomCenter
                            6 -> popupAlignment.value = Alignment.BottomStart
                            7 -> popupAlignment.value = Alignment.CenterStart
                            8 -> popupAlignment.value = Alignment.Center
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .background(color = Color.White),
            text = "Alignment : " + popupAlignment.value.toString()
        )
    }
}

@Composable
private fun ColumnScope.PopupWithEditText() {
    Column(Modifier.align(Alignment.CenterHorizontally)) {
        val widthSize = 190.dp
        val heightSize = 120.dp
        val editLineSize = 150.dp
        val showEmail = remember {
            mutableStateOf("Enter your email in the white rectangle and click outside")
        }
        val email = remember { mutableStateOf("email") }
        val showPopup = remember { mutableStateOf(true) }

        Text(text = showEmail.value)

        Box(
            modifier = Modifier.size(widthSize, heightSize)
                .align(Alignment.CenterHorizontally)
                .background(Color.Red)
        ) {
            if (showPopup.value) {
                Popup(
                    alignment = Alignment.Center,
                    properties = PopupProperties(focusable = true),
                    onDismissRequest = {
                        showEmail.value = "You entered: " + email.value
                        showPopup.value = false
                    }
                ) {
                    EditLine(
                        modifier = Modifier.width(editLineSize),
                        initialText = "",
                        color = Color.White,
                        onValueChange = {
                            email.value = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PopupWithChangingSize() {
    Column(Modifier.align(Alignment.CenterHorizontally)) {
        val showPopup = remember { mutableStateOf(true) }
        val heightSize = 120.dp
        val widthSize = 160.dp
        val rectangleState = remember { mutableStateOf(0) }

        Spacer(Modifier.height(15.dp))
        Box(
            modifier = Modifier.size(widthSize, heightSize).background(Color.Magenta)
        ) {
            if (showPopup.value) {
                Popup(Alignment.Center) {
                    val size = when (rectangleState.value % 4) {
                        0 -> Modifier.size(30.dp)
                        1 -> Modifier.size(100.dp)
                        2 -> Modifier.size(30.dp, 90.dp)
                        else -> Modifier.size(90.dp, 30.dp)
                    }
                    Box(modifier = size.background(Color.Gray))
                }
            }
        }
        Spacer(Modifier.height(25.dp))
        ClickableTextWithBackground(
            text = "Change size",
            color = Color.Cyan,
            onClick = {
                rectangleState.value += 1
            }
        )
    }
}

@Composable
private fun ColumnScope.PopupInsideScroller() {
    Column(
        Modifier.size(200.dp, 400.dp)
            .align(Alignment.CenterHorizontally)
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier.size(80.dp, 160.dp).background(Color(0xFF00FF00))
            ) {
                Popup(alignment = Alignment.Center) {
                    ClickableTextWithBackground(text = "Centered", color = Color.Cyan)
                }
            }

            for (i in 0..30) {
                Text(text = "Scroll #$i", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun PopupOnKeyboardUp() {
    Column {
        val widthSize = 190.dp
        val heightSize = 120.dp

        Spacer(Modifier.height(350.dp))
        Text("Start typing in the EditText below the parent(Red rectangle)")
        Box(
            modifier = Modifier.size(widthSize, heightSize)
                .align(Alignment.CenterHorizontally)
                .background(Color.Red)
        ) {
            Popup(Alignment.Center) {
                Box(Modifier.background(Color.Green)) {
                    Text("Popup")
                }
            }
        }

        EditLine(initialText = "Continue typing...", color = Color.Gray)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ClickableTextWithBackground(
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    padding: Dp = 0.dp
) {
    Box(
        Modifier
            .clickable(onClick = onClick ?: {}, enabled = onClick != null)
            .background(color)
            .padding(padding)
    ) {
        Text(text)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EditLine(
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onValueChange: (String) -> Unit = {},
    initialText: String = "",
    color: Color = Color.White
) {
    val state = remember { mutableStateOf(initialText) }
    BasicTextField(
        value = state.value,
        modifier = modifier.background(color = color),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        onValueChange = {
            state.value = it
            onValueChange(it)
        }
    )
}
