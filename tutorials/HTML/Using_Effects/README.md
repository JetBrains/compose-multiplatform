# Using Effects

The Compose HTML library introduces a few dom-specific effects on top of [existing effects from Compose](https://developer.android.com/jetpack/compose/side-effects).


### ref in AttrsBuilder

Under the hood, `ref` uses [DisposableEffect](https://developer.android.com/jetpack/compose/side-effects#disposableeffect)

`ref` can be used to retrieve a reference to a html element.
The lambda that `ref` takes in is not Composable. It will be called only once when an element added into a composition.
Likewise, the lambda passed in `onDispose` will be called only once when an element leaves the composition.

``` kotlin
Div(attrs = {
    ref { htmlDivElement ->
       // htmlDivElement is a reference to the HTMLDivElement
       onDispose {
          // add clean up code here
       }
    }
}) {
    // Content()
}
```

Only one `ref` can be used per element. Calling it more than once will dismiss earlier calls.

For example, `ref` can be used to add and remove some event listeners not provided out of the box.

### DisposableRefEffect (deprecated)

**Deprecated:** 

Consider using `DisposableEffect`. Its scope provides `scopeElement` - a reference to the underlying HTML element:
``` kotlin
DisposableEffect(key) {
    scopeElement.innerText = key
    onDispose { scopeElement.innerText = "" }
}
```
---
Under the hood, `DisposableRefEffect` uses [DisposableEffect](https://developer.android.com/jetpack/compose/side-effects#disposableeffect)

`DisposableRefEffect` is similar to `ref`, since it also provides a reference to an element. At the same time it has few differences.

- `DisposableRefEffect` can be added only within a content lambda of an element, while `ref` can be used only in `attrs` scope.
- Unlike `ref`, `DisposableRefEffect` can be used as many times as needed and every effect will be unique.
- DisposableRefEffect can be used with a `key` and without it.  When it's used with a `key: Any`, the effect will be disposed and reset when `key` value changes. When it's used without a key, then it behaves like `ref` - the effect gets called only once when an element enters the composition, and it's disposed only when the element leaves the composition.


``` kotlin
Div {
    // without a key
    DisposableRefEffect { htmlDivElement ->
        // htmlDivElement is a reference to the HTMLDivElement
        onDispose {
            // add clean up code here
        }
    }
}


var state by remember { mutableStateOf(1) }

Div {
    // with a key. 
    // The effect will be called for every new state's value
    DisposableRefEffect(state) { htmlDivElement ->
        // htmlDivElement is a reference to the HTMLDivElement
        onDispose {
            // add clean up code here
        }
    }
}
```

### DomSideEffect (deprecated)

**Deprecated:**

Consider using [SideEffect](https://developer.android.com/jetpack/compose/side-effects#sideeffect-publish).
If a reference to an underlying HTML element is needed, consider using `DisposableEffect` and `scopeElement` within its scope.

---
Under the hood, `DomSideEffect` uses [SideEffect](https://developer.android.com/jetpack/compose/side-effects#sideeffect-publish)

`DomSideEffect` as well as `DisposableRefEffect` can be used with a key and without it.

Unlike `DisposableRefEffect`, `DomSideEffect` without a key is invoked on every successful recomposition. 
With a `key`, it will be invoked only when the `key` value changes.

Same as [SideEffect](https://developer.android.com/jetpack/compose/side-effects#sideeffect-publish), `DomSideEffect` can be helpful when there is a need to update objects not managed by Compose.
In case of web, it often involves updating HTML nodes, therefore `DomSideEffect` provides a reference to an element in the lambda.

### Code Sample using effects

The code below showcases how it's possible to use non-composable components in Compose by applying `DomSideEffect` and `DisposableRefEffect`.

```kotlin
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLParagraphElement


// Here we pretend that `RedBoldTextNotComposableRenderer`
// wraps a UI logic provided by 3rd party library that doesn't use Compose

object RedBoldTextNotComposableRenderer {
    fun unmountFrom(root: HTMLElement) {
        root.removeChild(root.firstChild!!)
    }

    fun mountIn(root: HTMLElement) {
        val pElement = document.createElement("p") as HTMLParagraphElement
        pElement.setAttribute("style", "color: red; font-weight: bold;")
        root.appendChild(pElement)
    }

    fun renderIn(root: HTMLElement, text: String) {
        (root.firstChild as HTMLParagraphElement).innerText = text
    }
}

// Here we define a Composable wrapper for the above code. Here we use DomSideEffect and DisposableRefEffect. 
@Composable // @param `show: Boolean` was left here intentionally for the sake of the example
fun ComposableWrapperForRedBoldTextFrom3rdPartyLib(state: Int, show: Boolean) {
    Div(attrs = {
        style {
            backgroundColor(Color.lightgray)
            width(100.px)
            minHeight(40.px)
            padding(30.px)
        }
    }) {
        if (!show) {
            Text("No content rendered by the 3rd party library")
        }

        Div {
            if (show) {
                // Update the content rendered by "non-compose library" according to the `state`
                DomSideEffect(state) { div ->
                    RedBoldTextNotComposableRenderer.renderIn(div, "Value = $state")
                }
            }

            DisposableRefEffect(show) { div ->
                if (show) {
                    // Let "non-compose library" control the part of the page.
                    // The content of this div is independent of Compose. 
                    // It will be managed by RedBoldTextNotComposableRenderer 
                    RedBoldTextNotComposableRenderer.mountIn(div)
                }
                onDispose {
                    if (show) {
                        // Clean up the html created/managed by "non-compose library"
                        RedBoldTextNotComposableRenderer.unmountFrom(div)
                    }
                }
            }
        }
    }
}

fun main() {
    var state by mutableStateOf(0)
    var showUncontrolledElements by mutableStateOf(false)

    renderComposable(rootElementId = "root") {

        ComposableWrapperForRedBoldTextFrom3rdPartyLib(state = state, show = showUncontrolledElements)

        Div {
            Label(forId = "checkbox") {
                Text("Show/hide text rendered by 3rd party library")
            }

            CheckboxInput(checked = showUncontrolledElements) {
                id("checkbox")
                onInput {
                    showUncontrolledElements = it.value
                }
            }
        }

        Button(attrs = {
            onClick { state += 1 }
        }) {
            Text("Incr. count ($state)")
        }
    }
}
```
