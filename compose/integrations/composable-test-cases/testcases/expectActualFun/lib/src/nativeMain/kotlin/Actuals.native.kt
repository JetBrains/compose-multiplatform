import androidx.compose.runtime.Composable
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode

@Composable
actual fun SimpleComposable() {
    TextLeafNode("SimpleComposable-Native")
}

@Composable
actual fun WithDefaultIntParam(i: Int) {
    TextLeafNode("SimpleComposable-Native-$i")
}

@Composable
actual fun WithDefaultStringParam(s: String) {
    TextLeafNode("SimpleComposable-Native-$s")
}

actual fun TakesComposableLambda(l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Native", l)
    }
}

actual fun TakesComposableLambdaWithDefault(l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Native", l)
    }
}

actual fun TakesComposableLambdaWithDefaultInt(i: Int, l: @Composable () -> Unit) {
    savedComposableLambda = {
        TextContainerNode("Native-$i", l)
    }
}

@Composable
actual fun ExpectComposableDefaultValueProvidedByAnotherComposable(
    value: String,
    content: @Composable (v: String) -> Unit
) {
    content("Native-$value")
}

@Composable
actual fun UseRememberInDefaultValueOfExpectFun(
    value: String,
    content: @Composable (v: String) -> Unit
) {
    content("Native-$value")
}

@Composable
actual fun <T> ExpectWithTypeParameter(
    value: T,
    content: @Composable (T) -> Unit
) {
    TextContainerNode("Native") {
        content(value)
    }
}

@Composable
actual fun <T> ExpectWithTypeParameterAndDefaultLambda(
    value: T,
    transform: (T) -> T
) {
    TextContainerNode("Native") {
        TextLeafNode(transform(value).toString())
    }
}

@Composable
actual fun <T> ExpectWithTypeParameterAndDefaultComposableLambda(
    value: T,
    content: @Composable (T) -> Unit
) {
    TextContainerNode("Native") {
        content(value)
    }
}
