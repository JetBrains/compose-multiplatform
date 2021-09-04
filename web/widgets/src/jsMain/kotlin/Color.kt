import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.ui.ExperimentalComposeWebWidgetsApi
import org.jetbrains.compose.web.css.rgba

@ExperimentalComposeWebWidgetsApi
val Color.implementation
    get() = rgba(red, green, blue, alpha)