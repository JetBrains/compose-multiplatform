import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import common.LocalAppResources
import common.rememberAppResources

fun main() = application {
    CompositionLocalProvider(LocalAppResources provides rememberAppResources()) {
        NotepadApplication(rememberApplicationState())
    }
}