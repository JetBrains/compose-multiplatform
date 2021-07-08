// @Module:Main
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.Composer

fun main() {
    callComposable {

        FooTakesTypedComposableLambda { "text" }
        FooTakesTypedComposableLambda2(10) { it + 100 }
        FooTakesTypedExtesionComposableLambda<String, Any, Unit>("text", Any()) { }
    }
}

fun callComposable(content: @Composable () -> Unit) {
    val c = content
}

// @Module:Lib
import androidx.compose.runtime.Composable


@Composable
fun <T> FooTakesTypedComposableLambda(composable: @Composable () -> T) {
    composable()
}

@Composable
fun <T> FooTakesTypedComposableLambda2(t: T, composable: @Composable (T) -> T) {
    composable(t)
}

@Composable
fun <T, K, R> FooTakesTypedExtesionComposableLambda(t: T, k: K, composable: @Composable T.(K) -> R) {
    t.composable(k)
}
