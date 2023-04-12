# Events handling

You can add event listeners in the `attrs` block:

#### onClick
``` kotlin
Button(
    attrs = {
        onClick { event -> 
            // event is of `SyntheticMouseEvent` type    
            println("button clicked at ${event.movementX}, ${event.movementY}")
            
            val nativeEvent = event.nativeEvent // [MouseEvent](https://developer.mozilla.org/en/docs/Web/API/MouseEvent)
        }
    }
) {
    Text("Button")
}
```

#### onInput
``` kotlin
val text = remember { mutableStateOf("") }

TextArea(
    value = text.value,
    attrs = {
        onInput {
            text.value = it.value
        }
    }
)
```


#### Other event handlers

For events that don't have their own configuration functions in the `attrs` block, you can use `addEventListener` with the `name` of the event, `options`, and an pass an `eventListener` which receives a `SyntheticEvent`. In this example, we're defining the behavior of a `Form` element when it triggers the `submit` event:

``` kotlin
Form(attrs = {
    this.addEventListener("submit") {
        console.log("Hello, Submit!")
        it.preventDefault()
    }
})
```


There are more event listeners supported out of the box. You can find all supported event listeners in the [source code](https://github.com/JetBrains/compose-multiplatform/blob/master/html/core/src/jsMain/kotlin/org/jetbrains/compose/html/attributes/EventsListenerScope.kt).


### Runnable example

```kotlin
import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Button(
            attrs = {
                onClick { event ->
                    println("button clicked at ${event.movementX}, ${event.movementY}")
                }
            }
        ) {
            Text("Button")
        }

        val text = remember { mutableStateOf("") }

        TextArea(
            value = text.value,
            attrs = {
                onInput {
                    text.value = it.value
                }
            }
        )

        Span {
            Text("Typed text = ${text.value}")
        }
    }
}
```
