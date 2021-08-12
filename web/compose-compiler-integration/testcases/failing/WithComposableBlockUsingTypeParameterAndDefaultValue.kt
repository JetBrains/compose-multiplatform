// https://github.com/JetBrains/compose-jb/issues/774

import androidx.compose.runtime.Composable

fun main() {
    callComposable {
        Foo<String> {  }
        FooTakesTypedComposableLambda2("T")
        FooTakesTypedComposableLambda3("T")
    }
}

fun callComposable(content: @Composable () -> Unit) {
    // does nothing
}

class RouterState<C>

@Composable
fun <C : Any> Foo(block: @Composable (RouterState<C>) -> Unit = {}) {}

@Composable
fun <T> FooTakesTypedComposableLambda2(t: T, composable: @Composable (T) -> T = { t }) {
    composable(t)
}

@Composable
fun <T> FooTakesTypedComposableLambda3(t: T, composable: @Composable () -> T = { t }) {
    composable()
}
