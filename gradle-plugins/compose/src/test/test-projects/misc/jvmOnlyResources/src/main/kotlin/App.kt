import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import me.app.jvmonlyresources.generated.resources.Res
import me.app.jvmonlyresources.generated.resources.vector
import org.jetbrains.compose.resources.*

@Composable
fun App() {
    Image(
        painter = painterResource(Res.drawable.vector),
        contentDescription = null
    )
}
