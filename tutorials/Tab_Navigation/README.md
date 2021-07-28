# Tabbing navigation and keyboard focus

## What is covered

In this tutorial, we will show you how to use tabbing navigation between components via keyboard shortcuts `tab` and `shift + tab`.

## Default `Next/Previous` tabbing navigation

By default, `Next/Previous` tabbed navigation moves focus in composition order (in order of appearance), to see how this works, we can use some of the components that are already focusable by default:`TextField`, `OutlinedTextField`, `BasicTextField`, `CircularProgressIndicator`, `LinearProgressIndicator`.

```kotlin
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        state = WindowState(size = WindowSize(350.dp, 450.dp)),
        onCloseRequest = ::exitApplication
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(50.dp)
            ) {
                for (x in 1..5) {
                    val text = remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text.value,
                        singleLine = true,
                        onValueChange = { text.value = it },
                        modifier = Modifier.height(46.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
```

![default-tab-nav](default-tab-nav.gif)

To make a non-focusable component focusable, you need to apply `Modifier.focusable ()` to the component.

```kotlin
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent

fun main() = application {
    Window(
        state = WindowState(size = WindowSize(350.dp, 450.dp)),
        onCloseRequest = ::exitApplication
    ) {
        MaterialTheme(
            colors = MaterialTheme.colors.copy(
                primary = Color(10, 132, 232),
                secondary = Color(150, 232, 150)
            )
        ) {
            val clicks = remember { mutableStateOf(0) }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(40.dp)
                ) {
                    Text(text = "Clicks: ${clicks.value}")
                    Spacer(modifier = Modifier.height(20.dp))
                    for (x in 1..5) {
                        FocusableButton("Button $x", { clicks.value++ })
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FocusableButton(
    text: String = "",
    onClick: () -> Unit = {},
    size: IntSize = IntSize(200, 35)
) {
    val keyPressedState = remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = if (interactionSource.collectIsFocusedAsState().value) {
            if (keyPressedState.value)
                lerp(MaterialTheme.colors.secondary, Color(64, 64, 64), 0.3f)
            else
                MaterialTheme.colors.secondary
        } else {
            MaterialTheme.colors.primary
        }
    )
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.size(size.width.dp, size.height.dp)
            .onPreviewKeyEvent {
                if (
                    it.key == Key.Enter ||
                    it.key == Key.Spacebar
                ) {
                    when (it.type) {
                        KeyEventType.KeyDown -> {
                            keyPressedState.value = true
                        }
                        KeyEventType.KeyUp -> {
                            keyPressedState.value = false
                            onClick.invoke()
                        }
                    }
                }
                false
            }
            .focusable(interactionSource = interactionSource),
        colors = colors
    ) {
        Text(text = text)
    }
}
```

![focusable-buttons](focusable-button.gif)

## Custom ordering
To move focus in custom order we need to create a `FocusRequester` link and apply the `Modifier.focusOrder` modifier to each component you want to navigate.

- `FocusRequester` sends requests to change focus.
- `Modifier.focusOrder` is used to specify a custom focus traversal order.

In the example below, we simply create a `FocusRequester` list and create text fields for each `FocusRequester` in the list. Each text field sends a focus request to the previous and next text field in the list when using the `shift + tab` or `tab` keyboard shortcut in reverse order.

```kotlin
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize

fun main() = application {
    Window(
        state = WindowState(size = WindowSize(350.dp, 450.dp)),
        onCloseRequest = ::exitApplication
    ) {
        val itemsList = remember { List(5) { FocusRequester() } }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(50.dp)
            ) {
                itemsList.forEachIndexed { index, item ->
                    val text = remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text.value,
                        singleLine = true,
                        onValueChange = { text.value = it },
                        modifier = Modifier.height(46.dp).focusOrder(item) {
                            // reverse order
                            next = if (index - 1 < 0) itemsList.last() else itemsList[index - 1]
                            previous = if (index + 1 == itemsList.size) itemsList.first() else itemsList[index + 1]
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
```
![reverse-order](reverse-order.gif)
