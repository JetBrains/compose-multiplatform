import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Message private constructor(
    val user: User,
    val timeMs: Long,
    val text: String,
    val id: Long
) {
    constructor(
        user: User,
        timeMs: Long,
        text: String
    ) : this(
        user = user,
        timeMs = timeMs,
        text = text,
        id = Random.nextLong()
    )
}

data class User(
    val name: String,
    val pictureColor: Color = Color(
        red = Random.nextInt(0xff),
        green = Random.nextInt(0xff),
        blue = Random.nextInt(0xff)
    ),
)
