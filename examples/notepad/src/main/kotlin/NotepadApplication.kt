import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.MenuScope
import androidx.compose.ui.window.Tray
import common.LocalAppResources
import kotlinx.coroutines.launch
import window.NotepadWindow

@Composable
fun NotepadApplication(state: NotepadApplicationState) {
    if (state.settings.isTrayEnabled && state.windows.isNotEmpty()) {
        ApplicationTray(state)
    }

    for (window in state.windows) {
        key(window) {
            NotepadWindow(window)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ApplicationTray(state: NotepadApplicationState) {
    Tray(
        LocalAppResources.current.icon ?: return,
        state = state.tray,
        hint = "Notepad",
        menu = { ApplicationMenu(state) }
    )
}

@Composable
private fun MenuScope.ApplicationMenu(state: NotepadApplicationState) {
    val scope = rememberCoroutineScope()
    fun exit() = scope.launch { state.exit() }

    Item("New", onClick = state::newWindow)
    Separator()
    Item("Exit", onClick = { exit() })
}