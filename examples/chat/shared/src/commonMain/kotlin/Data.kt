import androidx.compose.ui.graphics.Color
import kotlin.random.Random
import kotlin.random.nextInt

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
    val color: Color = ColorProvider.getColor(),
    val picture: String?
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
        println(colors.lastIndex)
        val idx = Random.nextInt(colors.indices)
        val color = colors[idx]
        colors.removeAt(idx)
        return Color(color)
    }
}