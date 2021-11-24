// https://github.com/JetBrains/compose-jb/issues/774
// fixed in https://github.com/JetBrains/androidx/pull/118

import androidx.compose.runtime.Composable

val set = mutableSetOf<Int>()

fun main() {
    callComposable {
        Foo<String> { set.add(1) }
        Foo<String>()
        FooTakesTypedComposableLambda2("T")
        FooTakesTypedComposableLambda3("T")
    }
    require(setOf(1,2,3,4) == set) { "Failed when running composable. Actual result - ${set.joinToString()}" }
}

class RouterState<C>

@Composable
fun <C : Any> Foo(block: @Composable (RouterState<C>) -> Unit = { set.add(2) }) {
    block(RouterState())
}

@Composable
fun <T> FooTakesTypedComposableLambda2(t: T, composable: @Composable (T) -> T = {
    set.add(3)
    t
}) {
    composable(t)
}

@Composable
fun <T> FooTakesTypedComposableLambda3(t: T, composable: @Composable () -> T = {
    set.add(4)
    t
}) {
    composable()
}
