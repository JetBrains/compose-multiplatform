# Keyboard events handling

## Prerequisites

This tutorial expects that you have already set up the Compose project as described in the [Getting Started tutorial](../Getting_Started)

## What is covered

In this tutorial, we will look at two different ways of handling keyboard events in Compose for Desktop as well as the utilities that we have to do this.

## KeySets

Compose for Desktop has a few utilities to work with shortcuts:

`KeysSet` represents a set of keys that can be simultaneously pressed. You can construct a KeysSet using the Key's extension function:

``` kotlin
Key.CtrlLeft + Key.Enter
```

## Event handlers

There are two ways to handle key events in Compose for Desktop:

- By setting up an event handler based on the element that is in focus
- By setting up an event handler in the scope of the window

## Focus related events

It works the same as Compose for Android, for details see [API Reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/key/package-summary#keyinputfilter)

`Modifier.shortcuts` is used to define one or multiple callbacks for `KeysSet`s.

The most common use case is to define keyboard handlers for active controls like `TextField`. Here is an example:

``` kotlin
import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.shortcuts

@OptIn(ExperimentalKeyInput::class)
fun main() = Window(title = "Compose for Desktop", size = IntSize(300, 300)) {
    MaterialTheme {
        var consumedText by remember { mutableStateOf(0) }
        var text by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Text("Consumed text: $consumedText")
            TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.shortcuts {
                        on(Key.CtrlLeft + Key.Minus) {
                            consumedText -= text.length
                            text = ""
                        }
                        on(Key.CtrlLeft + Key.Equals) {
                            consumedText += text.length
                            text = ""
                        }
                    }
            )
        }
    }
}
```


Note the annotation `@OptIn(ExperimentalKeyInput::class)`. Keyboard-related event handlers are still an experimental feature of Compose, and later API changes are possible. So it requires the use of a special annotation to emphasize the experimental nature of the code.

![keyInputFilter](keyInputFilter.gif)

## Window-scoped events

`AppWindow` instances have a `keyboard` property. It is possible to use it to define keyboard shortcuts that are always active in the current window. Here is an example:

``` kotlin
import androidx.compose.desktop.AppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalKeyInput::class)
fun main() = Window(title = "Compose for Desktop", size = IntSize(300, 300)) {
    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Button(
                    modifier = Modifier.padding(4.dp),
                    onClick = {
                        AppWindow(size = IntSize(200, 200)).also {
                            it.keyboard.setShortcut(Key.Escape) {
                                it.close()
                            }
                        }.show {
                            Text("I'm popup!")
                        }
                    }
            ) {
                Text("Open popup")
            }
        }
    }
}
```

![window_keyboard](window_keyboard.gif)
