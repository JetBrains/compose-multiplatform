import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val myUser = User("Me")
val friends = listOf(User("Alex"), User("Lily"), User("Sam"))
val friendMessages = listOf(
    "Hi, have a nice day!",
    "Nice to see you!",
    "Multiline\ntext\nmessage"
)

@Composable
internal fun ChatApp(store: Store, android:Boolean = false) {
    val state by store.stateFlow.collectAsState()
    MaterialTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.weight(1f)) {
                        Messages(state.messages)
                    }
                    if (android) {
                        SendMessage { text ->
                            store.send(
                                Action.SendMessage(
                                    Message(myUser, timeMs = timestampMs(), text)
                                )
                            )
                        }
                    } else {
                        Box(Modifier.height(250.dp))
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            store.send(
                Action.SendMessage(
                    message = Message(
                        user = friends.random(),
                        timeMs = timestampMs(),
                        text = friendMessages.random()
                    )
                )
            )
            delay(5000)
        }
    }
}
