import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.web.css.rgba

val Color.implementation
    get() = rgba(red, green, blue, alpha)