import androidx.compose.ui.window.Application
import androidx.compose.ui.main.defaultIOSMain

fun main() {
    defaultIOSMain("Chat", Application("Chat") {
        ChatApp()
    })
}
