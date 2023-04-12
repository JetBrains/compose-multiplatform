# Using test-utils for unit testing

### Dependencies

It's necessary to add  `compose.html.testUtils` to jsTest dependencies:

``` kotlin
sourceSets {
    val jsMain by getting {
        dependencies {
            implementation(compose.html.core)
            implementation(compose.runtime)
            //....
        }
    }
    val jsTest by getting {
        implementation(kotlin("test-js"))
        implementation(compose.html.testUtils)
        //...
    }
}
```


### Example

``` kotlin
// This is a function that we want to test
@Composable
fun TestButton(text: String, onButtonClick: () -> Unit) {
    Button(attrs = {
        onClick { onButtonClick() }
    }) {
        Text(text)
    }
}
```

Let's add a test to ensure that button has correct text, and it's onClick works properly.
``` kotlin
import org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi
import org.jetbrains.compose.web.testutils.runTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.w3c.dom.HTMLButtonElement
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ComposeWebExperimentalTestsApi::class)
class TestsForButton {

    @Test
    fun testButton() = runTest {
        var counter by mutableStateOf(1)
        
        composition {
            TestButton(text = "$counter") {
                counter++
            }
        }

        assertEquals("<button>1</button>", root.innerHTML)

        (root.firstChild!! as HTMLButtonElement).click()
        waitForRecompositionComplete()
        assertEquals("<button>2</button>", root.innerHTML)

        counter = 10
        waitForRecompositionComplete()
        assertEquals("<button>10</button>", root.innerHTML)
    }
}
```

### Let's break it down:

### `runTest { ... }` 
Provides the TestScope with useful functions to configure the test.

### `composition { ... }`
Takes a @Composable block with a content that we want to test.
It will automatically build and mount DOM into `root` element.

### `root`
It's not supposed to be used for elements manipulation.
It's mostly useful to make assertions on the html content (e.g. `root.innerHtml`)

### `nextChild() and currentChild()`
Under the hood `nextChild()` iterates over `root` children, providing convenient access to them.

`currentChild()` doesn't move the iterator and returns the same element every time until `nextChild()` called. 

### `waitForRecompositionComplete()` 
It suspends until recomposition completes. It's useful when state changes, and we want to test that content updates as well. `waitForRecompositionComplete` needs to be called after state change and before assertions.

### `waitForChanges(id: String)`
It suspends until any change occur in the element with `id`.
It's also useful to ensure that state changes make corresponding updates to the content.
