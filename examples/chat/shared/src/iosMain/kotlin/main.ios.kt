import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun ChatViewController(): UIViewController = ComposeUIViewController {
    ChatApp(displayTextField = false)
}

fun sendMessage(text: String) {
    store.send(Action.SendMessage(Message(myUser, text)))
}

fun gradient3Colors() = ChatColors.GRADIENT_3

fun surfaceColor() = ChatColors.SURFACE
