import androidx.compose.desktop.Window
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun main() {
    Window {
        Column(Modifier.fillMaxSize(), Arrangement.SpaceEvenly) {
            Text(
                text = "Привет! 你好! Desktop Compose",
                color = Color.Black,
                modifier = Modifier
                    .background(Color.Blue)
                    .preferredHeight(56.dp)
                    .wrapContentSize(Alignment.Center)
            )
        }
    }
}