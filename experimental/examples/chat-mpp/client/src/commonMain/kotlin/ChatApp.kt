import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
fun ChatApp() {
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
                    NewMessage { text ->
                        store.send(
                            Action.SendMessage(
                                Message(myUser, time = currentTime(), text)
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
                        time = currentTime(),
                        text = friendMessages.random()
                    )
                )
            )
            delay(5000)
        }
    }
}

@Composable
fun NewMessage(sendMessage: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    TextField(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(10.dp),
        value = inputText,
        placeholder = {
            Text("type message here")
        },
        onValueChange = {
            inputText = it
        },
        trailingIcon = {
            if (inputText.isNotEmpty()) {
                Icon(
                    modifier = Modifier
                        .clickable {
                            sendMessage(inputText)
                            inputText = ""
                        }
                        .padding(10.dp),
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    )
}

@Composable
fun Messages(messages: List<Message>) {
    val listState = rememberLazyListState()
    if (messages.isNotEmpty()) {
        LaunchedEffect(messages.size) {
            listState.animateScrollToItem(messages.lastIndex, scrollOffset = 2)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
    ) {
        items(messages) { message ->
            ChatMessage(isMyMessage = message.user == myUser, message)
        }
    }
}

@Composable
fun ChatMessage(isMyMessage: Boolean, message: Message) {
    val imageSize = 64f

    @Composable
    fun UserPic() {
        Image(
            modifier = Modifier
                .size(imageSize.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            painter = object : Painter() {
                override val intrinsicSize: Size = Size(imageSize, imageSize)
                override fun DrawScope.onDraw() {
                    drawRect(message.user.pictureColor, size = Size(imageSize * 4, imageSize * 4))
                }
            },
            contentDescription = "User picture"
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.padding(4.dp)
                .align(if (isMyMessage) Alignment.CenterStart else Alignment.CenterEnd),
            shape = RoundedCornerShape(size = 20.dp),
            elevation = 8.dp
        ) {
            Box(
                Modifier.background(brush = Brush.horizontalGradient(listOf(Color(0xff8888ff), Color(0xffddddff))))
                    .padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    if (isMyMessage) {
                        UserPic()
                        Spacer(Modifier.size(8.dp))
                    }
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = message.user.name,
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = message.time,
                                style = MaterialTheme.typography.h6
                            )
                        }
                        Text(
                            text = message.text
                        )
                    }
                    if (!isMyMessage) {
                        Spacer(Modifier.size(8.dp))
                        UserPic()
                    }
                }
            }
        }
        if (!isMyMessage) {
            var liked by remember { mutableStateOf(false) }
            Icon(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .clickable {
                        liked = !liked
                    }
                    .padding(4.dp),
                imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                contentDescription = "Like",
                tint = if (liked) Color.Red else Color.Gray
            )
        }
    }
}
