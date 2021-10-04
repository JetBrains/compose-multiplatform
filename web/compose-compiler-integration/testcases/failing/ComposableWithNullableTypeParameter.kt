// @Module:Main

// https://github.com/JetBrains/compose-jb/issues/1226

import kotlin.reflect.KProperty
import androidx.compose.runtime.Composable

@Composable
fun <T> MySelect(
    options: List<T>,
    onChange: (T?) -> Unit
) {

}

fun main() {
    callComposable {
        MySelect<String>(
            options = emptyList(),
            onChange = {}
        )
    }
}

fun callComposable(content: @Composable () -> Unit) {
    // does nothing
}
