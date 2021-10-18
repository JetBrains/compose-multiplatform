// @Module:Main
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.Composer

fun main() {
    callComposable {
        ComposableWithDefaultParamsDefinedByOtherParams("a")
    }
}

fun callComposable(content: @Composable () -> Unit) {
    val c = content
}

// @Module:Lib
import androidx.compose.runtime.Composable

@Composable
fun ComposableWithDefaultParamsDefinedByOtherParams(
    a: String,
    b: String = a
) {
}
