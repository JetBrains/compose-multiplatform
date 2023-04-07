// @Module:Main

// https://github.com/JetBrains/compose-jb/issues/1226

// TODO: move this to passing cases after kotlin 1.6.0
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
