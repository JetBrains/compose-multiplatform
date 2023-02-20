import androidx.compose.ui.window.Application
import platform.UIKit.UIViewController

fun ChatViewController(): UIViewController =
    Application("Chat") {
        ChatApp(displayTextField = false)
    }

fun sendMessage(text: String) {
    store.send(Action.SendMessage(Message(myUser, timestampMs(), text)))
}

fun gradient3Colors() = ChatColors.GRADIENT_3

fun surfaceColor() = ChatColors.SURFACE
