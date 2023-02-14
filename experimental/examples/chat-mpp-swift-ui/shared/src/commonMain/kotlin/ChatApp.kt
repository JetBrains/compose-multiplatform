import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

val myUser = User("Me")
val friends = listOf(User("Alex"), User("Lily"), User("Sam"))
val friendMessages = listOf(
    "Hi, have a nice day!",
    "Nice to see you!",
    "Multiline\ntext\nmessage"
)

@Composable
internal fun ChatApp() {
    val coroutineScope = rememberCoroutineScope()
    val store = remember { coroutineScope.createStore() }
    val state by store.stateFlow.collectAsState()

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Chat sample") }
                    )
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.weight(1f)) {
                        Messages(state.messages)
                    }
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
