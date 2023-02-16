import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

val myUser = User("Me")
val friends = listOf(User("Alex"), User("Lily"), User("Sam"))
val friendMessages = listOf(
    "Hi, have a nice day!",
    "Nice to see you!",
    "Multiline\ntext\nmessage"
)
val store = CoroutineScope(SupervisorJob()).createStore()

@Composable
internal fun ChatApp(displayTextField:Boolean = true) {
    val state by store.stateFlow.collectAsState()
    MaterialTheme(
        colors = darkColors(
            surface = Color(ChatColors.SURFACE),
            background = Color(ChatColors.BACKGROUND),
        ),
    ) {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.weight(1f)) {
                        Messages(state.messages, displayStub = !displayTextField)
                    }
                    if (displayTextField) {
                        SendMessage { text ->
                            store.send(
                                Action.SendMessage(
                                    Message(myUser, timeMs = timestampMs(), text)
                                )
                            )
                        }
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
