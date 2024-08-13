import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.random.Random
import kotlin.random.nextInt

data class Message(
    val user: User,
    val text: String,
    val seconds: Long,
    val id: Long
) {
    constructor(
        user: User,
        text: String
    ) : this(
        user = user,
        text = text,
        seconds = Clock.System.now().epochSeconds,
        id = Random.nextLong()
    )
}

data class User(
    val name: String,
    val color: Color = ColorProvider.getColor(),
    val picture: DrawableResource?
)

object ColorProvider {
    val colors = mutableListOf(
        0xFFEA3468,
        0xFFB634EA,
        0xFF349BEA,
    )
    val allColors = colors.toList()
    fun getColor(): Color {
        if(colors.size == 0) {
            colors.addAll(allColors)
        }
        val idx = Random.nextInt(colors.indices)
        val color = colors[idx]
        colors.removeAt(idx)
        return Color(color)
    }
}