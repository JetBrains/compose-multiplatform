# Getting Started with Compose for Desktop

## What is covered

In this tutorial we will see how to install mouse even listeners on components
in Compose for Desktop.

## Mouse event listeners

Let's create a text, and install pointer move filter on it:
```kotlin
import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp


fun main() = Window(title = "Compose for Desktop", size = IntSize(400, 400)) {
    var overText = remember { mutableStateOf("Move mouse over text:") }
    Text(
            text = overText.value,
            fontSize = 40.sp,
            modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .pointerMoveFilter(
                    onMove = {
                        if (it.x > 10 && it.y > 10)
                            overText.value = "Move position: $it"
                        false
                    },
                    onEnter = {
                        overText.value = "Over enter"
                        false
                    },
                    onExit = {
                        overText.value = "Over exit"
                        false
                    }
            )
    )
}

```
