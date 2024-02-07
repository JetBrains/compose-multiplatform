import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.group.resources_test.generated.resources.Res
import org.jetbrains.compose.resources.*

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    Column {
        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(Res.drawable.vector),
            contentDescription = null
        )
        Text(stringResource(Res.string.app_name))
        val font = FontFamily(Font(Res.font.emptyFont))
    }
}
