import androidx.compose.ui.window.Application
import androidx.compose.ui.main.defaultUIKitMain

fun main() {
    defaultUIKitMain("Chat", Application("Chat") {
        ChatApp()
    })
}
