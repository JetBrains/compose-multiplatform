import com.example.compose.common.LoginScreen
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        LoginScreen()
    }
}
