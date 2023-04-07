// @Module:Main
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.Composer

fun main() {
    callComposable {
        ComposableWithDefaultParamsDefinedByOtherParams("a")
    }
    require(result == "aa") { "Actual result was - $result"}
}


// @Module:Lib
import androidx.compose.runtime.Composable

var result = ""

@Composable
fun ComposableWithDefaultParamsDefinedByOtherParams(
    a: String,
    b: String = a
) {
    result = a + b
}
