# Events handling in Compose Web

**The API is experimental and breaking changes can be expected**

Events listeners can be added in `attrs` block:

#### onClick
```kotlin
Button(
    attrs = {
        onClick { wrappedMouseEvent -> 
            // wrappedMouseEvent is of `WrappedMouseEvent` type    
            println("button clicked at ${wrappedMouseEvent.movementX}, ${wrappedMouseEvent.movementY}")
            
            val nativeEvent = wrappedMouseEvent.nativeEvent // [MouseEvent](https://developer.mozilla.org/en/docs/Web/API/MouseEvent)
        }
    }
) {
    Text("Button")
}
```

#### onInput
```kotlin
val text = remember { mutableStateOf("") }

TextArea(
    value = text.value,
    attrs = {
        onTextInput { wrappedTextInputEvent ->
            // wrappedTextInputEvent is of `WrappedTextInputEvent` type
            text.value = wrappedTextInputEvent.inputValue
        }
    }
)
```

All wrapped event types inherit from `GenericWrappedEvent`, therefore one can access `nativeEvent`. 
Native Event is the instance that is actually created by JS runtime - 
https://developer.mozilla.org/en-US/docs/Web/API/Event


There are more event listeners supported out of a box. We plan to add the documentation for them later on.
In the meantime, all supported event listeners can be found in the [source code](https://github.com/JetBrains/androidx/blob/compose-web-main/compose/web/src/jsMain/kotlin/androidx/compose/web/attributes/EventsListenerBuilder.kt).