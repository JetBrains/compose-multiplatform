import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
internal inline fun Messages(messages: List<Message>) {
    val listState = rememberLazyListState()
    if (messages.isNotEmpty()) {
        LaunchedEffect(messages.last()) {
            listState.animateScrollToItem(messages.lastIndex, scrollOffset = 2)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
    ) {
        messages.forEach { message ->
            item(key = message.id) {
                ChatMessage(isMyMessage = message.user == myUser, message)
            }
        }
        item {
            Box(Modifier.height(50.dp))
        }
    }
}

@Composable
private inline fun ChatMessage(isMyMessage: Boolean, message: Message) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier.padding(4.dp),
            shape = RoundedCornerShape(size = 20.dp),
            elevation = 8.dp
        ) {
            Box(
                Modifier.background(brush = Brush.horizontalGradient(
                    ChatColors.GRADIENT_2.map { Color(it) })
                ).padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    if (!isMyMessage) {
                        UserPic(message.user)
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
                                text = timeToString(message.timeMs),
                                style = MaterialTheme.typography.h6
                            )
                        }
                        Text(
                            text = message.text
                        )
                    }
                    if (isMyMessage) {
                        Spacer(Modifier.size(8.dp))
                        UserPic(message.user)
                    }
                }
            }
            if (!isMyMessage) {
                var liked by remember { mutableStateOf(false) }
                Icon(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .clickable {
                            liked = !liked
                            focusManager.clearFocus(true)
                        }
                        .padding(3.dp),
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                    contentDescription = "Like",
                    tint = if (liked) Color.Red else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun UserPic(user: User) {
    val imageSize = 64f
    Image(
        modifier = Modifier
            .size(imageSize.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        painter = object : Painter() {
            override val intrinsicSize: Size = Size(imageSize, imageSize)
            override fun DrawScope.onDraw() {
                drawRect(user.pictureColor, size = Size(imageSize * 4, imageSize * 4))
            }
        },
        contentDescription = "User picture"
    )
}
