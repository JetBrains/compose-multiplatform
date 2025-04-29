/*
 * Copyright 2023 The Android Open Source Project
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

// copy of https://github.com/JetBrains/compose-multiplatform-core/blob/d9e875b62e7bb4dd47b6b155d3a787251ff5bd38/compose/desktop/desktop/samples/src/jvmMain/kotlin/androidx/compose/desktop/examples/example1/Main.jvm.kt#L17

package benchmarks.multipleComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomAppBar
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import compose_benchmarks.benchmarks.generated.resources.Res
import compose_benchmarks.benchmarks.generated.resources.example1_cat
import compose_benchmarks.benchmarks.generated.resources.example1_ic_call_answer
import compose_benchmarks.benchmarks.generated.resources.example1_sailing
import compose_benchmarks.benchmarks.generated.resources.jetbrainsmono_italic
import compose_benchmarks.benchmarks.generated.resources.jetbrainsmono_regular
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

@Composable
fun MultipleComponentsExample(isVectorGraphicsSupported: Boolean = true) {
    val uriHandler = LocalUriHandler.current
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isVectorGraphicsSupported) {
                                Image(
                                    painterResource(Res.drawable.example1_sailing),
                                    contentDescription = "Star"
                                )
                            }
                            Text("Desktop Compose Elements")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Open URL") },
                    onClick = {
                        uriHandler.openUri("https://google.com")
                    }
                )
            },
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar(cutoutShape = CircleShape) {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(Icons.Filled.Menu, "Menu", Modifier.size(ButtonDefaults.IconSize))
                    }
                }
            },
            content = { innerPadding ->
                Row(Modifier.padding(innerPadding)) {
                    LeftColumn(Modifier.weight(1f))
                    MiddleColumn(Modifier.width(500.dp), isVectorGraphicsSupported = isVectorGraphicsSupported)
                    RightColumn(Modifier.width(200.dp))
                }
            }
        )
    }
}

@Composable
private fun LeftColumn(modifier: Modifier) = Box(modifier.fillMaxSize()) {
    val state = rememberScrollState()
    ScrollableContent(state)

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ScrollableContent(scrollState: ScrollState) {
    val italicFont = FontFamily(
        Font(Res.font.jetbrainsmono_italic)
    )

    val dispatchedFonts = FontFamily(
        Font(Res.font.jetbrainsmono_regular, style = FontStyle.Italic),
        Font(Res.font.jetbrainsmono_italic, style = FontStyle.Normal)
    )

    val amount = remember { mutableStateOf(0f) }
    val animation = remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text(
            text = "Привет! 你好! Desktop Compose use: ${amount.value}",
            color = Color.Black,
            modifier = Modifier
                .background(Color.Blue)
                .height(56.dp)
                .wrapContentSize(Alignment.Center)
        )

        val inlineIndicatorId = "indicator"

        Text(
            text = buildAnnotatedString {
                append("The quick ")
                if (animation.value) {
                    appendInlineContent(inlineIndicatorId)
                }
                pushStyle(
                    SpanStyle(
                        color = Color(0xff964B00),
                        shadow = Shadow(Color.Green, offset = Offset(1f, 1f))
                    )
                )
                append("brown fox")
                pop()
                pushStyle(SpanStyle(background = Color.Yellow))
                append(" 🦊 ate a ")
                pop()
                pushStyle(SpanStyle(fontSize = 30.sp, textDecoration = Underline))
                append("zesty hamburgerfons")
                pop()
                append(" 🍔.\nThe 👩‍👩‍👧‍👧 laughed.")
                addStyle(SpanStyle(color = Color.Green), 25, 35)
            },
            color = Color.Black,
            inlineContent = mapOf(
                inlineIndicatorId to InlineTextContent(
                    Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                    )
                ) {
                    CircularProgressIndicator(Modifier.padding(end = 3.dp))
                }
            )
        )

        val loremColors = listOf(
            Color.Black,
            Color.Yellow,
            Color.Green,
            Color.Blue
        )
        var loremColor by remember { mutableStateOf(0) }

        val loremDecorations = listOf(
            TextDecoration.None,
            TextDecoration.Underline,
            TextDecoration.LineThrough
        )
        val lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
                " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad" +
                " minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
                " aliquipex ea commodo consequat. Duis aute irure dolor in reprehenderit" +
                " in voluptate velit esse cillum dolore eu fugiat nulla pariatur." +
                " Excepteur" +
                " sint occaecat cupidatat non proident, sunt in culpa qui officia" +
                " deserunt mollit anim id est laborum."
        var loremDecoration by remember { mutableStateOf(0) }
        Text(
            text = lorem,
            color = loremColors[loremColor],
            textAlign = TextAlign.Center,
            textDecoration = loremDecorations[loremDecoration],
            modifier = Modifier.clickable {
                if (loremColor < loremColors.size - 1) {
                    loremColor += 1
                } else {
                    loremColor = 0
                }

                if (loremDecoration < loremDecorations.size - 1) {
                    loremDecoration += 1
                } else {
                    loremDecoration = 0
                }
            }
        )

        Text(
            text = lorem,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row {
            Box(modifier = Modifier.size(200.dp, 200.dp).border(1.dp, Black)) {
                BrushTextGradient(lorem)
            }
            Box(modifier = Modifier.size(200.dp, 200.dp).border(1.dp, Black)) {
                BrushTextImage(lorem)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "Default",
            )

            Text(
                "SansSerif",
                fontFamily = FontFamily.SansSerif
            )

            Text(
                "Serif",
                fontFamily = FontFamily.Serif
            )

            Text(
                "Monospace",
                fontFamily = FontFamily.Monospace
            )

            Text(
                "Cursive",
                fontFamily = FontFamily.Cursive
            )
        }

        var overText by remember { mutableStateOf("Move mouse over text:") }
        Text(overText, style = TextStyle(letterSpacing = 10.sp))

        SelectionContainer {
            Text(
                text = "fun <T : Comparable<T>> List<T>.quickSort(): List<T> = when {\n" +
                        "  size < 2 -> this\n" +
                        "  else -> {\n" +
                        "    val pivot = first()\n" +
                        "    val (smaller, greater) = drop(1).partition { it <= pivot }\n" +
                        "    smaller.quickSort() + pivot + greater.quickSort()\n" +
                        "   }\n" +
                        "}",
                fontFamily = italicFont,
                modifier = Modifier
                    .padding(10.dp)
                    .onPointerEvent(PointerEventType.Move) {
                        val position = it.changes.first().position
                        overText = "Move position: $position"
                    }
                    .onPointerEvent(PointerEventType.Enter) {
                        overText = "Over enter"
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        overText = "Over exit"
                    }
            )
        }
        Text(
            text = buildAnnotatedString {
                append("resolved: NotoSans-Regular.ttf ")
                pushStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic
                    )
                )
                append("NotoSans-italic.ttf.")
            },
            fontFamily = dispatchedFonts,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                modifier = Modifier.padding(4.dp).pointerHoverIcon(PointerIcon.Hand),
                onClick = {
                    amount.value++
                }
            ) {
                Text("Base")
            }

            var clickableText by remember { mutableStateOf("Click me!") }
            Text(
                modifier = Modifier.mouseClickable(
                    onClick = {
                        clickableText = buildString {
                            append("Buttons pressed:\n")
                            append("primary: ${buttons.isPrimaryPressed}\t")
                            append("secondary: ${buttons.isSecondaryPressed}\t")
                            append("tertiary: ${buttons.isTertiaryPressed}\t")
                            append("primary: ${buttons.isPrimaryPressed}\t")
                            append("back: ${buttons.isBackPressed}\t")
                            append("forward: ${buttons.isForwardPressed}\t")

                            append("\n\nKeyboard modifiers pressed:\n")

                            append("alt: ${keyboardModifiers.isAltPressed}\t")
                            append("ctrl: ${keyboardModifiers.isCtrlPressed}\t")
                            append("meta: ${keyboardModifiers.isMetaPressed}\t")
                            append("shift: ${keyboardModifiers.isShiftPressed}\t")
                        }
                    }
                ),
                text = clickableText
            )
        }

        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Column {
                    Switch(
                        animation.value,
                        onCheckedChange = {
                            animation.value = it
                        }
                    )
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            animation.value,
                            onCheckedChange = {
                                animation.value = it
                            }
                        )
                        Text("Animation")
                    }
                }

                Button(
                    modifier = Modifier.padding(4.dp),
                    onClick = { }
                ) {
                    Text("Window")
                }
            }

            Animations(isCircularEnabled = animation.value)
        }

        Slider(
            value = amount.value / 100f,
            onValueChange = { amount.value = (it * 100) }
        )
        val dropDownMenuExpanded = remember { mutableStateOf(false) }
        Button(onClick = { dropDownMenuExpanded.value = true }) {
            Text("Expand Menu")
        }
        DropdownMenu(
            expanded = dropDownMenuExpanded.value,
            onDismissRequest = {
                dropDownMenuExpanded.value = false
                println("OnDismissRequest")
            }
        ) {
            DropdownMenuItem(modifier = Modifier, onClick = {
                println("Item 1 clicked")
            }) {
                Text("Item 1")
            }
            DropdownMenuItem(modifier = Modifier, onClick = {
                println("Item 2 clicked")
            }) {
                Text("Item 2")
            }
            DropdownMenuItem(modifier = Modifier, onClick = {
                println("Item 3 clicked")
            }) {
                Text("Item 3")
            }
        }
        TextField(
            value = amount.value.toString(),
            onValueChange = { amount.value = it.toFloatOrNull() ?: 42f },
            label = { Text(text = "Input1") }
        )

    }
}

@Composable
fun MiddleColumn(modifier: Modifier, isVectorGraphicsSupported: Boolean) = Column(modifier) {
    val (focusItem1, focusItem2) = FocusRequester.createRefs()
    val text = remember {
        mutableStateOf("Hello \uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0")
    }

    TextField(
        value = text.value,
        onValueChange = { text.value = it },
        label = { Text(text = "Input2") },
        placeholder = {
            Text(text = "Important input")
        },
        maxLines = 1,
        modifier = Modifier.onPreviewKeyEvent {
            when {
                (it.isMetaPressed && it.key == Key.Enter) -> {
                    if (it.isShiftPressed) {
                        text.value = "Cleared with shift!"
                    } else {
                        text.value = "Cleared!"
                    }
                    true
                }
                else -> false
            }
        }.focusRequester(focusItem1)
            .focusProperties {
                next = focusItem2
            }
    )

    var text2 by remember {
        val initText = buildString {
            (1..1000).forEach {
                append("$it\n")
            }
        }
        mutableStateOf(initText)
    }
    TextField(
        text2,
        modifier = Modifier
            .height(200.dp)
            .focusRequester(focusItem2)
            .focusProperties {
                previous = focusItem1
            },
        onValueChange = { text2 = it }
    )

    Row {
        Image(
            painterResource(Res.drawable.example1_cat),
            "Localized description",
            Modifier.size(200.dp)
        )

        if (isVectorGraphicsSupported) {
            Icon(
                painterResource(Res.drawable.example1_ic_call_answer),
                "Localized description",
                Modifier.size(100.dp).align(Alignment.CenterVertically),
                tint = Color.Blue.copy(alpha = 0.5f)
            )
        }
    }

    Box(
        modifier = Modifier.size(150.dp).background(Color.Gray).pointerHoverIcon(
            PointerIcon.Hand
        )
    ) {
        Box(
            modifier = Modifier.offset(20.dp, 20.dp).size(100.dp).background(Color.Blue).pointerHoverIcon(
                PointerIcon.Crosshair
            )
        ) {
            Text("pointerHoverIcon test with Ctrl")
        }
    }
}

@Composable
fun Animations(isCircularEnabled: Boolean) = Row {
    if (isCircularEnabled) {
        CircularProgressIndicator(Modifier.padding(10.dp))
    }

    val enabled = remember { mutableStateOf(true) }
    val color by animateColorAsState(
        if (enabled.value) Color.Green else Color.Red,
        animationSpec = TweenSpec(durationMillis = 2000)
    )

    MaterialTheme {
        Box(
            Modifier
                .size(70.dp)
                .clickable { enabled.value = !enabled.value }
                .background(color)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RightColumn(modifier: Modifier) = Box(modifier) {
    val state = rememberLazyListState()
    val itemCount = 100000
    val heights = remember {
        val random = Random(24)
        (0 until itemCount).map { random.nextFloat() }
    }

    LazyColumn(Modifier.fillMaxSize().graphicsLayer(alpha = 0.5f), state = state) {
        items((0 until itemCount).toList()) { i ->
            val itemHeight = 20.dp + 20.dp * heights[i]
            Text(i.toString(), Modifier.graphicsLayer(alpha = 0.5f).height(itemHeight))
        }
    }

    VerticalScrollbar(
        rememberScrollbarAdapter(state),
        Modifier.align(Alignment.CenterEnd)
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun BrushTextGradient(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            brush = Brush.linearGradient(
                colors = listOf(Red, Yellow, Green, Blue)
            )
        )
    )
}

@OptIn(ExperimentalTextApi::class, ExperimentalResourceApi::class, InternalResourceApi::class)
@Composable
fun BrushTextImage(text: String) {
    val image: ImageBitmap? by produceState(null) {
        value = Res.readBytes("files/example1_compose-community-primary.png").decodeToImageBitmap()
    }
    if (image != null) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                brush = ShaderBrush(
                    ImageShader(image!!, TileMode.Repeated, TileMode.Repeated)
                )
            )
        )
    }
}