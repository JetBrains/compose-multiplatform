// @Module:Main

// https://github.com/JetBrains/compose-jb/issues/1306 - compilation error
// https://github.com/JetBrains/compose-jb/issues/1436 - probably a behaviour bug

import kotlin.reflect.KProperty
import androidx.compose.runtime.*


fun main() {
    var called = false

    val (foo, setFoo) = mutableStateOf<(@Composable () -> Unit)>({
        Text("Hello!")
    })

    // Ensure this at least compiles.
    // Text("Hello World!") won't be invoked - https://github.com/JetBrains/compose-jb/issues/1436
    setFoo { Text("Hello World!") }

    callComposable {
        foo()
        called = true
    }

    require(called) { "Failed when running composable" }
}

@Composable
fun Text(text: String) {
}
