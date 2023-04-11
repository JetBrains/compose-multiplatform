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
    require(setOf(1, 2, 3, 4, 5, 6, 7) == set) { "Failed when running composables - ${set.joinToString()}" }
}

// @Module:Lib
import androidx.compose.runtime.Composable

var set = mutableSetOf<Int>()

@Composable
fun FooTakesLambda(block: () -> Unit = { set.add(1) }) {
    block()
}

@Composable
inline fun InlineFooTakesLambda(block: () -> Unit = { set.add(2) }) {
    block()
}

@Composable
fun FooTakesComposableLambda(composable: @Composable () -> Unit = { set.add(3) }) {
    composable()
}

@Composable
inline fun InlineFooTakesComposableLambda(composable: @Composable () -> Unit = { set.add(4) }) {
    composable()
}

@Composable
fun FooTakesTypedExtesionComposableLambdaWithExplicitTypesAndDefaultLambda(
    t: String, k: Int, composable: @Composable String.(Int) -> Double = {
        set.add(5)
        (this + ".$it").toDouble()
    }
) {
    t.composable(k)
}

@Composable
fun ComposableWithDifferentDefaultValuesForParameters(
    a: Any, i: Int = 1, b: Boolean = false, s: String = "s",
    u: Unit = Unit, a2: Any = Any(), l: List<Any> = listOf("1")
) {
    set.add(6)
    a.toString() + "$i $b $s $u $a2 $l"
}

@Composable
fun ComposableWithReturnAndWithDefaultLambda(
    l: @Composable () -> (@Composable () -> Unit) = { { set.add(7) } }
): @Composable () -> Unit {
    return { l().invoke() }
}
