
**Run in browser:**

`./gradlew jsRun`

### How to use HTML based @Composable functions in JS?

Useful links:
- [Use Kotlin Code from JS](https://kotlinlang.org/docs/js-to-kotlin-interop.html)
- [JavaScript modules](https://kotlinlang.org/docs/js-modules.html)
- [webpack bundling](https://kotlinlang.org/docs/js-project-setup.html#webpack-bundling)

1) @Composable functions can't be invoked from JS directly (because every @Composable function has implicit parameters added by compiler plugin)
2) We can wrap @Composable functions into some usual function (with [@JsExport](https://kotlinlang.org/docs/js-to-kotlin-interop.html#jsexport-annotation)) which can be invoked in JS as is.
3) Every call to a "wrapping" function will create a composition (part of DOM controlled by Compose runtime)
4) It's important to `dispose` a composition when it's not needed.
5) The composition's state can be controlled by creating an arbitrary `Controller` class, which implements and exposes necessary functions for state updates.


### Simplified example (see full example in `src/jsMain`):

```kotlin
// Composables.kt

@JsExport
abstract class ComposeCounterAppController {
    abstract fun setCount(newCount: Int)
    abstract fun dispose()
}

@JsExport
fun ComposeCounterApp(rootId: String, onCountChange: (Int) -> Unit = {}): ComposeCounterAppController {
    var count: Int by mutableStateOf(0)

    val composition = renderComposable(rootElementId = rootId) {
        // Counter is a @Composable function
        // see full example in src/jsMain
        Counter(count) {
            count = it
            onCountChange(count)
        }
    }

    return object : ComposeCounterAppController() {
        override fun setCount(newCount: Int) {
            count = newCount
        }

        override fun dispose() {
            composition.dispose()
        }
    }
}
```

Then in JS we can use `ComposeCounterApp` function:

```javascript
// see src/jsMain/resources/use_compose.js file for a full example

counterController = MyComposables.ComposeCounterApp('counterByCompose', (newCount) => {
    console.log(`Counter was updated. New value = ${newCount}`);
});
```

The module name was overridden to make it convenient for usage in JS:

```
// webpack.config.d/configModuleName.js

config.output = config.output || {};
config.output.library = "MyComposables";
```

### Running Web browser application
* To run, launch command: `./gradlew :jsBrowserDevelopmentRun`
* Or choose **browser** configuration in IDE and run it.  
  ![browser-run-configuration.png](screenshots/browser-run-configuration.png)

### Building and using the output

```
./gradlew jsBrowserProductionWebpack
```

This will produce the output in `build/distributions`.
Then we can use exported functions from `compose-in-js.js` (the filename is defined by the project/module name).
```html
<script src="compose-in-js.js"></script>
<script src="use_compose.js"></script>
```
