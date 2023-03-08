package my.abc

import androidx.compose.runtime.Composable
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode

@Composable
actual fun SimpleComposable() {
    TextLeafNode("SimpleComposable-Web")
}

@Composable
actual fun WithDefaultIntParam(i: Int) {
    TextLeafNode("SimpleComposable-Web-$i")
}

@Composable
actual fun WithDefaultStringParam(s: String) {
    TextLeafNode("SimpleComposable-Web-$s")
}

actual fun TakesComposableLambda(l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Web", l)
    }
}

actual fun TakesComposableLambdaWithDefault(l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Web", l)
    }
}

actual fun TakesComposableLambdaWithDefaultInt(i: Int, l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Web-$i", l)
    }
}

@Composable
actual fun ExpectComposableDefaultValueProvidedByAnotherComposable(
    value: String,
    content: @Composable (v: String) -> Unit
) {
    content("Web-$value")
}


@Composable
actual fun UseRememberInDefaultValueOfExpectFun(
    value: String,
    content: @Composable (v: String) -> Unit
) {
    content("Web-$value")
}

@Composable
actual fun <T> ExpectWithTypeParameter(
    value: T,
    content: @Composable (T) -> Unit
) {
    TextContainerNode("Web") {
        content(value)
    }
}

@Composable
actual fun <T> ExpectWithTypeParameterAndDefaultLambda(
    value: T,
    transform: (T) -> T
) {
    TextContainerNode("Web") {
        TextLeafNode(transform(value).toString())
    }
}

@Composable
actual fun <T> ExpectWithTypeParameterAndDefaultComposableLambda(
    value: T,
    content: @Composable (T) -> Unit
) {
    TextContainerNode("Web") {
        content(value)
    }
}

@Composable
actual fun <T> ExpectWithTypeParameterInReturnAndDefaultComposableLambda(
    value: T,
    calculate: @Composable (T) -> T
): T {
    return calculate(value)
}

actual class ExpectClass actual constructor() {

    @Composable
    actual fun ExpectComposableFunWithDefaultInt(i: Int) {
        TextLeafNode("Web(i = $i)")
    }
}

actual class ExpectClassWithString actual constructor() {

    @Composable
    actual fun ExpectComposableFunWithDefaultComposableLambda(
        s: String, transform: @Composable (String) -> String
    ) {
        TextLeafNode("Web(s = ${transform(s)})")
    }
}

actual class ExpectClassWithStringProperty actual constructor(s: String) {

    actual val property: String = s

    @Composable
    actual fun ExpectComposableFunWithDefaultComposableLambda(
        transform: @Composable (String) -> String
    ) {
        TextLeafNode("Web(s = ${transform(property)})")
    }
}

actual class ExpectClassWithT<T> actual constructor() {

    @Composable
    actual fun ExpectComposableFunWithDefaultComposableLambda(t: T, transform: @Composable (T) -> T) {
        TextLeafNode("Web(t = ${transform(t)})")
    }
}

actual class ExpectClassWithTProp<T> actual constructor(t: T) {

    actual val tVal: T = t

    @Composable
    actual fun ExpectComposableFunWithDefaultComposableLambda(transform: @Composable (T) -> T) {
        TextLeafNode("Web(tProp = ${transform(tVal)})")
    }
}

actual class ExpectClassWithTProp2<T> actual constructor(t: T) {

    actual val tVal: T = t

    @Composable
    actual fun ExpectComposableFunWithDefaultComposableLambda(t: T) {
        TextLeafNode("Web(tProp = $t)")
    }
}