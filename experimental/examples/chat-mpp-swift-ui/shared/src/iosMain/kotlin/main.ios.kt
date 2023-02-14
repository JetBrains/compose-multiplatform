import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import platform.UIKit.UIViewController

val store = CoroutineScope(SupervisorJob()).createStore()

fun MainViewController(): UIViewController =
    Application("Chat") {
        ChatApp(store)
    }

fun sendMessage(text: String) {
    store.send(Action.SendMessage(Message(myUser, timestampMs(), text)))
}
