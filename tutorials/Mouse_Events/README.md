# Mouse events

## What is covered

In this tutorial we will see how to install mouse event listeners on components
in Compose for Desktop.

## Mouse event listeners

### Click listeners

Click listeners are available in both Compose on Android and Compose for Desktop, so code like this will work on both platforms:

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication

fun main() = singleWindowApplication {
    var count by remember { mutableStateOf(0) }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        var text by remember { mutableStateOf("Click magenta box!") }
        Column {
            @OptIn(ExperimentalFoundationApi::class)
            Box(
                modifier = Modifier
                    .background(Color.Magenta)
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.2f)
                    .combinedClickable(
                        onClick = {
                            text = "Click! ${count++}"
                        },
                        onDoubleClick = {
                            text = "Double click! ${count++}"
                        },
                        onLongClick = {
                            text = "Long click! ${count++}"
                        }
                    )
            )
            Text(text = text, fontSize = 40.sp)
        }
    }
}
```

<img alt="Application running" src="mouse_click.gif" height="500" />

Please note, that advanced click events processing is available on Desktop via AWT interop. See **Advanced click events processing** section below.  

### Mouse move listeners

Let's create a window and install a pointer move listener on it that changes the background color according to the mouse pointer position:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication {
    var color by remember { mutableStateOf(Color(0, 0, 0)) }
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.Center)
            .fillMaxSize()
            .background(color = color)
            .onPointerEvent(PointerEventType.Move) {
                val position = it.changes.first().position
                color = Color(position.x.toInt() % 256, position.y.toInt() % 256, 0)
            }
    )
}
```
*Note that onPointerEvent is experimental and can be changed in the future. For more stable API look at [Modifier.pointerInput](#listenining-raw-events-in-commonmain-via-modifierpointerinput)*.

<img alt="Application running" src="mouse_move.gif" height="519" />

### Mouse enter listeners

Compose for Desktop also supports pointer enter and exit handlers, like this:
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication {
    Column(
        Modifier.background(Color.White),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(10) { index ->
            var active by remember { mutableStateOf(false) }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = if (active) Color.Green else Color.White)
                    .onPointerEvent(PointerEventType.Enter) { active = true }
                    .onPointerEvent(PointerEventType.Exit) { active = false },
                fontSize = 30.sp,
                fontStyle = if (active) FontStyle.Italic else FontStyle.Normal,
                text = "Item $index"
            )
        }
    }
}
```
*Note that onPointerEvent is experimental and can be changed in the future. For more stable API look at [Modifier.pointerInput](#listenining-raw-events-in-commonmain-via-modifierpointerinput)*.

<img alt="Application running" src="mouse_enter.gif" height="500" />

### Mouse scroll listeners
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication {
    var number by remember { mutableStateOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) {
                number += it.changes.first().scrollDelta.y
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Scroll to change the number: $number", fontSize = 30.sp)
    }
}
```
*Note that onPointerEvent is experimental and can be changed in the future. For more stable API look at [Modifier.pointerInput](#listenining-raw-events-in-commonmain-via-modifierpointerinput)*.

### Mouse right/middle clicks and keyboard modifiers

Compose for Desktop contains desktop-only `Modifier.mouseClickable`, where data about pressed mouse buttons and keyboard modifiers is available. This is an experimental API, which means that it's likely to be changed before release.

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalFoundationApi::class)
fun main() = singleWindowApplication {
    var clickableText by remember { mutableStateOf("Click me!") }

    Text(
        modifier = Modifier.mouseClickable(
            onClick = {
                clickableText = buildString {
                    append("Buttons pressed:\n")
                    append("primary: ${buttons.isPrimaryPressed}\t")
                    append("secondary: ${buttons.isSecondaryPressed}\t")
                    append("tertiary: ${buttons.isTertiaryPressed}\t")

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
```
<img alt="Application running" src="mouse_event.gif" height="500" />

If you need to listen left/right clicks simultaneously, you should listen for raw events:
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication {
    var text by remember { mutableStateOf("Press me") }

    Box(
        Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Press) {
                val position = it.changes.first().position
                text = when {
                    it.buttons.isPrimaryPressed &&
                            it.buttons.isSecondaryPressed -> "Left+Right click $position"
                    it.buttons.isSecondaryPressed -> "Right click $position"
                    it.buttons.isPrimaryPressed -> "Left click $position"
                    else -> text
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 30.sp)
    }
}
```
*Note that onPointerEvent is experimental and can be changed in the future. For more stable API look at [Modifier.pointerInput](#listenining-raw-events-in-commonmain-via-modifierpointerinput)*.

### Swing interoperability

Compose for Desktop uses Swing underneath and allows to access raw AWT events:
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() = singleWindowApplication {
    var text by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Press) {
                text = it.awtEventOrNull?.locationOnScreen?.toString().orEmpty()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}
```
*Note that onPointerEvent is experimental and can be changed in the future. For more stable API look at [Modifier.pointerInput](#listenining-raw-events-in-commonmain-via-modifierpointerinput)*.

### Listenining raw events in commonMain via Modifier.pointerInput
In the snippets above we use `Modifier.onPointerEvent`, which is a helper function to subscribe to some type of pointer events. It is a shorter variant of `Modifier.pointerInput`. For now it is experimental, and desktop-only (you can't use it in commonMain code). If you need to subscribe to events in commonMain or you need stable API, you can use `Modifier.pointerInput`:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.singleWindowApplication

fun main() = singleWindowApplication {
    val list = remember { mutableStateListOf<String>() }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position
                        // on every relayout Compose will send synthetic Move event,
                        // so we skip it to avoid event spam
                        if (event.type != PointerEventType.Move) {
                            list.add(0, "${event.type} $position")
                        }
                    }
                }
            },
    ) {
        for (item in list.take(20)) {
            Text(item)
        }
    }
}
```

### Advanced click events processing (only for Desktop-JVM platform)
_NB: Please note, that approach described below is temporary and is to be replaced by Compose API in future!_

It is possible to get additional information about mouse event, like number of clicks or state of other mouse buttons at the click time, via awt event. 

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.isPrimaryPressed
import java.awt.event.MouseEvent

@androidx.compose.ui.ExperimentalComposeUiApi
fun main() = singleWindowApplication {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        var text by remember { mutableStateOf("Click magenta box!") }
        Column {
            @OptIn(ExperimentalFoundationApi::class)
            Box(
                modifier = Modifier
                    .background(Color.Magenta)
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.2f)
                    .onPointerEvent(PointerEventType.Press) {
                        when(it.awtEventOrNull?.button) {
                            MouseEvent.BUTTON1 ->
                                when (it.awtEventOrNull?.clickCount) {
                                    1 -> { text = "Single click"}
                                    2 -> { text = "Double click"}
                                }
                            MouseEvent.BUTTON3 -> { //BUTTON3 is right button
                                if (it.buttons.isPrimaryPressed) { text = "Right + left click" }
                                else { text = "Right click" }
                            }
                        }
                    }
            )
            Text(text = text, fontSize = 40.sp)
        }
    }
}
```

### New experimental onClick handlers (only for Desktop-JVM platform)
`Modifier.onClick` provides independent callbacks for clicks, double clicks, long clicks. It handles clicks originated only from pointer events and accessibility `click` event is not handled out of a box.

Each `onClick` can be configured to target specific pointer events (using `matcher: PointerMatcher` and `keyboardModifiers: PointerKeyboardModifiers.() -> Boolean`). `matcher` can be specified to choose what mouse button should trigger a click. `keyboardModifiers` allows for filtering pointer events which have specified keyboardModifiers pressed.   
   
Multiple `onClick` modifiers can be chained to handle different clicks with different conditions (matcher and keyboard modifiers).   
Unlike `clickable`, `onClick` doesn't have a default `Modifier.indication` and `Modifier.semantics`, which need to be added separately if necessary.   
The most generic (with the least number of conditions) `onClick` handlers should be declared before other to ensure correct events propagation.

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
fun main() {
    singleWindowApplication {
        Column {
            var topBoxText by remember { mutableStateOf("Click me") }
            var topBoxCount by remember { mutableStateOf(0) }
            // No indication on interaction
            Box(modifier = Modifier.size(200.dp, 100.dp).background(Color.Blue)
                // the most generic click handler (without extra conditions) should be the first one
                .onClick {
                    // it will receive all LMB clicks except when Shift is pressed
                    println("Click with primary button")
                    topBoxText = "LMB ${topBoxCount++}"
                }.onClick(
                    keyboardModifiers = { isShiftPressed } // accept clicks only when Shift pressed
                ) {
                    // it will receive all LMB clicks when Shift is pressed
                    println("Click with primary button and shift pressed")
                    topBoxCount++
                    topBoxText = "LMB + Shift ${topBoxCount++}"
                }
            ) {
                AnimatedContent(
                    targetState = topBoxText,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(text = it)
                }
            }

            var bottomBoxText by remember { mutableStateOf("Click me") }
            var bottomBoxCount by remember { mutableStateOf(0) }
            val interactionSource = remember { MutableInteractionSource() }
            // With indication on interaction
            Box(modifier = Modifier.size(200.dp, 100.dp).background(Color.Yellow)
                .onClick(
                    enabled = true,
                    interactionSource = interactionSource,
                    matcher = PointerMatcher.mouse(PointerButton.Secondary), // Right Mouse Button
                    keyboardModifiers = { isAltPressed }, // accept clicks only when Alt pressed
                    onLongClick = { // optional
                        bottomBoxText = "RMB Long Click + Alt ${bottomBoxCount++}"
                        println("Long Click with secondary button and Alt pressed")
                    },
                    onDoubleClick = { // optional
                        bottomBoxText = "RMB Double Click + Alt ${bottomBoxCount++}"
                        println("Double Click with secondary button and Alt pressed")
                    },
                    onClick = {
                        bottomBoxText = "RMB Click + Alt ${bottomBoxCount++}"
                        println("Click with secondary button and Alt pressed")
                    }
                )
                .onClick(interactionSource = interactionSource) { // use default parameters
                    bottomBoxText = "LMB Click ${bottomBoxCount++}"
                    println("Click with primary button (mouse left button)")
                }
                .indication(interactionSource, LocalIndication.current)
            ) {
                AnimatedContent(
                    targetState = bottomBoxText,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(text = it)
                }
            }
        }
    }
}
```

### New experimental onDrag modifier (only for Desktop-JVM platform)

`Modifier.onDrag` allows for configuration of the pointer that should trigger the drag (see `matcher: PointerMatcher`).   
Many `onDrag` modifiers can be chained together.  

The example below also shows how to access the state of keyboard modifiers (via `LocalWindowInfo.current.keyboardModifier`) for cases when keyboard modifiers can alter the behaviour of the drag (for example: move an item if simple drag; or copy/paste an item if dragged with Ctrl pressed)

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    singleWindowApplication {
        val windowInfo = LocalWindowInfo.current

        Column {
            var topBoxOffset by remember { mutableStateOf(Offset(0f, 0f)) }

            Box(modifier = Modifier.offset {
                IntOffset(topBoxOffset.x.toInt(), topBoxOffset.y.toInt())
            }.size(100.dp)
                .background(Color.Green)
                .onDrag { // all default: enabled = true, matcher = PointerMatcher.Primary (left mouse button)
                    topBoxOffset += it
                }
            ) {
                Text(text = "Drag with LMB", modifier = Modifier.align(Alignment.Center))
            }

            var bottomBoxOffset by remember { mutableStateOf(Offset(0f, 0f)) }

            Box(modifier = Modifier.offset {
                IntOffset(bottomBoxOffset.x.toInt(), bottomBoxOffset.y.toInt())
            }.size(100.dp)
                .background(Color.LightGray)
                .onDrag(
                    enabled = true,
                    matcher = PointerMatcher.mouse(PointerButton.Secondary), // right mouse button
                    onDragStart = {
                        println("Gray Box: drag start")
                    },
                    onDragEnd = {
                        println("Gray Box: drag end")
                    }
                ) {
                    val keyboardModifiers = windowInfo.keyboardModifiers
                    bottomBoxOffset += if (keyboardModifiers.isCtrlPressed) it * 2f else it
                }
            ){
                Text(text = "Drag with RMB,\ntry with CTRL", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
```

There is also a non-modifier way to handle drags using `suspend fun PointerInputScope.detectDragGestures`:

```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    singleWindowApplication {
        var topBoxOffset by remember { mutableStateOf(Offset(0f, 0f)) }

        Box(modifier = Modifier.offset {
            IntOffset(topBoxOffset.x.toInt(), topBoxOffset.y.toInt())
        }.size(100.dp)
            .background(Color.Green)
            .pointerInput(Unit) {
                detectDragGestures(
                    matcher = PointerMatcher.Primary
                ) {
                    topBoxOffset += it
                }
            }
        ) {
            Text(text = "Drag with LMB", modifier = Modifier.align(Alignment.Center))
        }
    }
}
```

