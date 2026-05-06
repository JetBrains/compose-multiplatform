import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun ChatViewController(): UIViewController = ComposeUIViewController {
    ChatApp(
        displayTextField = false,
        contentPadding = WindowInsets.systemBars.asPaddingValues(),
    )
}

fun sendMessage(text: String) {
    store.send(Action.SendMessage(Message(myUser, text)))
}

fun gradient3Colors() = ChatColors.GRADIENT_3

fun surfaceColor() = ChatColors.SURFACE
