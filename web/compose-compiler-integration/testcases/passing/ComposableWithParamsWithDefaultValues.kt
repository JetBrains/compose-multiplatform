// @Module:Main
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.Composer

fun main() {
    callComposable {
        FooTakesLambda()
        InlineFooTakesLambda()

        FooTakesComposableLambda()
        InlineFooTakesComposableLambda()

        FooTakesTypedExtesionComposableLambdaWithExplicitTypesAndDefaultLambda("4", 5)
        ComposableWithDifferentDefaultValuesForParameters(a = Any())
        ComposableWithReturnAndWithDefaultLambda().invoke()
    }
}

fun callComposable(content: @Composable () -> Unit) {
    val c = content
}

// @Module:Lib
import androidx.compose.runtime.Composable

@Composable
fun FooTakesLambda(block: () -> Unit = {}) {
    block()
}

@Composable
inline fun InlineFooTakesLambda(block: () -> Unit = {}) {
    block()
}

@Composable
fun FooTakesComposableLambda(composable: @Composable () -> Unit = {}) {
    composable()
}

@Composable
inline fun InlineFooTakesComposableLambda(composable: @Composable () -> Unit = {}) {
    composable()
}

@Composable
fun FooTakesTypedExtesionComposableLambdaWithExplicitTypesAndDefaultLambda(
    t: String, k: Int, composable: @Composable String.(Int) -> Double = { (this + ". $it").toDouble() }
) {
    t.composable(k)
}

@Composable
fun ComposableWithDifferentDefaultValuesForParameters(
    a: Any, i: Int = 1, b: Boolean = false, s: String = "s",
    u: Unit = Unit, a2: Any = Any(), l: List<Any> = listOf("1")
) {
    a.toString() + "$i $b $s $u $a2 $l"
}

@Composable
fun ComposableWithReturnAndWithDefaultLambda(
    l: @Composable () -> (@Composable () -> Unit) = { { } }
): @Composable () -> Unit {
    return { l() }
}
