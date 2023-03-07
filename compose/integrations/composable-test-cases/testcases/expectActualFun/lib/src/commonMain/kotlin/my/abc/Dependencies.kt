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
