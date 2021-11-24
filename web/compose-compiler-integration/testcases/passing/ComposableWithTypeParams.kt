// @Module:Main
import androidx.compose.runtime.*

fun main() {
    var set = mutableSetOf<Int>()
    callComposable {
        FooTakesTypedComposableLambda {
            set.add(1)
            "text"
        }
        FooTakesTypedComposableLambda2(10) {
            set.add(2)
            it + 100
        }
        FooTakesTypedExtesionComposableLambda<String, Any, Unit>("text", Any()) {
            set.add(3)
        }
        MySelect<String>(listOf("1")) {
            set.add(4)
        }
    }

    require(setOf(1, 2, 3, 4) == set) { "Failed when running composables" }
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

@Composable
fun <T> MySelect(
    options: List<T>,
    onChange: (T) -> Unit
) {
    onChange(options.first())
}
