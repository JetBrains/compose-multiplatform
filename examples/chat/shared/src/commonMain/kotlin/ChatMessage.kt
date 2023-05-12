import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

@Composable
fun Photo() {
    Box(Modifier.size(32.dp).background(Color.Red)) {

    }
}

@Composable
inline fun ChatMessage(isMyMessage: Boolean, message: Message) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {

        Row(verticalAlignment = Alignment.Bottom) {
            if (!isMyMessage) {
                Column {
                    UserPic(message.user)
                }
                Spacer(Modifier.size(2.dp))
                Column {
                    Box(Modifier.padding(bottom = 10.dp, start = 0.dp).clip(TriangleEdgeShape(!isMyMessage)).background(if(!isMyMessage) Color.White else Color(0xFFE5FEFB)).size(6.dp))
                }
            }

            Column {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp, 10.dp, if(!isMyMessage) 10.dp else 0.dp, if(!isMyMessage) 0.dp else 10.dp))
                        .background(color = if(!isMyMessage) Color.White else Color(0xFFE5FEFB))
                        .padding(start = 10.dp, top = 5.dp, end = 10.dp, bottom = 5.dp),
                ) {
                    Column {
                        if(!isMyMessage) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = message.user.name,
                                    style = MaterialTheme.typography.body1.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.sp,
                                        fontSize = 0.9.em
                                    ),
                                    color = message.user.pictureColor //Color(0xFFEA8034)
                                )
                            }
                        }
                        Spacer(Modifier.size(3.dp))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.body1.copy(fontSize = 1.1.em, letterSpacing = 0.sp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = timeToString(message.timeMs),
                                textAlign = TextAlign.End,
                                style = MaterialTheme.typography.subtitle1.copy(fontSize = 0.6.em),
                                color = Color(0xFF979797)
                            )
                        }
                    }
                }
                Box(Modifier.size(10.dp))
            }
            if(isMyMessage) {
                Column {
                    Box(Modifier.padding(bottom = 10.dp, start = 0.dp).clip(TriangleEdgeShape(!isMyMessage)).background(if(!isMyMessage) Color.White else Color(0xFFE5FEFB)).size(6.dp))
                }
            }
        }
    }
}



// Adapted from https://stackoverflow.com/questions/65965852/jetpack-compose-create-chat-bubble-with-arrow-and-border-elevation
class TriangleEdgeShape(val risingToTheRight: Boolean) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val trianglePath = if(risingToTheRight) {
            Path().apply {
                moveTo(x = 0f, y = size.height)
                lineTo(x = size.width, y = 0f)
                lineTo(x = size.width, y = size.height)
            }
        } else {
            Path().apply {
                moveTo(x = 0f, y = 0f)
                lineTo(x = size.width, y = size.height)
                lineTo(x = 0f, y = size.height)
            }
        }

        return Outline.Generic(path = trianglePath)
    }
}