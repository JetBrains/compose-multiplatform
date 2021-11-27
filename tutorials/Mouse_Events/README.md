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

If you need to listen left/right clicks simultaneously, you should listen raw events:
```
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
```
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
import androidx.compose.ui.awt.awtEvent
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
                text = it.awtEvent.locationOnScreen?.toString().orEmpty()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}
```
*Note that onPointerEvent is experimental and can be changed in the future. For more stable API look at [Modifier.pointerInput](#listenining-raw-events-in-commonmain-via-modifierpointerinput)*.

### Listenining raw events in commonMain via Modifier.pointerInput
Multiplatform source set commonMain doesn't support experimental `Modifier.onPointerInput` yet. Instead of it you can use stable Modifier.pointerInput:

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