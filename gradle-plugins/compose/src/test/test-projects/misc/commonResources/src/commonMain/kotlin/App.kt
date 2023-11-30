import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import generated.resources.Res
import org.jetbrains.compose.resources.*

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    Column {
        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(Res.images.vector),
            contentDescription = null
        )
        Text(getString(Res.strings.app_name))
        val font = FontFamily(Font(Res.fonts.emptyfont))
    }
}
