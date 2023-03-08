package my.abc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode

@Composable
expect fun SimpleComposable()

@Composable
expect fun WithDefaultIntParam(i: Int = 10)

@Composable
expect fun WithDefaultStringParam(s: String = "defaultStringValue")

var savedComposableLambda: (@Composable () -> Unit)? = null

expect fun TakesComposableLambda(l: @Composable () -> Unit)

expect fun TakesComposableLambdaWithDefault(l: @Composable () -> Unit = { TextLeafNode("Default") })


fun TakesComposableLambdaWithDefaultIntNotExpect(i: Int = 100, l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Common-$i", l)
    }
}
expect fun TakesComposableLambdaWithDefaultInt(i: Int = 100, l: @Composable () -> Unit)

@Composable
fun defaultStringValueComposable(): String {
    return "defaultStringValueComposable"
}

@Composable
expect fun ExpectComposableDefaultValueProvidedByAnotherComposable(
    value: String = defaultStringValueComposable(),
    content: @Composable (v: String) -> Unit
)

@Composable
expect fun UseRememberInDefaultValueOfExpectFun(
    value: String = remember { "defaultRememberedValue" },
    content: @Composable (v: String) -> Unit
)

@Composable
expect fun <T> ExpectWithTypeParameter(
    value: T,
    content: @Composable (T) -> Unit //= { TextLeafNode(value.toString()) }
)

@Composable
expect fun <T> ExpectWithTypeParameterAndDefaultLambda(
    value: T,
    transform: (T) -> T = { it }
)

@Composable
expect fun <T> ExpectWithTypeParameterAndDefaultComposableLambda(
    value: T,
    content: @Composable (T) -> Unit = createDefaultContent(value)
)

fun <T> createDefaultContent(value: T): @Composable (T) -> Unit {
    return { TextLeafNode(value.toString()) }
}

@Composable
expect fun <T> ExpectWithTypeParameterInReturnAndDefaultComposableLambda(
    value: T,
    calculate: @Composable (T) -> T = { value }
): T

expect class ExpectClass() {

    @Composable
    fun ExpectComposableFunWithDefaultInt(i: Int = 11011)
}

expect class ExpectClassWithString() {

    @Composable
    fun ExpectComposableFunWithDefaultComposableLambda(
        s: String,
        transform: @Composable (String) -> String = { s }
    )
}

expect class ExpectClassWithStringProperty constructor(s: String) {

    val property: String

    @Composable
    fun ExpectComposableFunWithDefaultComposableLambda(
        transform: @Composable (String) -> String = { property }
    )
}

expect class ExpectClassWithT<T>() {

    @Composable
    fun ExpectComposableFunWithDefaultComposableLambda(t: T, transform: @Composable (T) -> T = { t })
}

expect class ExpectClassWithTProp<T>(t: T) {

    val tVal: T

    @Composable
    fun ExpectComposableFunWithDefaultComposableLambda(transform: @Composable (T) -> T = { tVal })
}

expect class ExpectClassWithTProp2<T>(t: T) {

    val tVal: T

    @Composable
    fun ExpectComposableFunWithDefaultComposableLambda(t: T = tVal)
}