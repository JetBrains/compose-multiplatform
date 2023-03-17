import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication

fun main() = singleWindowApplication {
    Column {
        Text("Text text text", style = TextStyle(fontSize = 30.sp))
        Text("Text text text", style = TextStyle(fontSize = 30.sp, textDecoration = TextDecoration.LineThrough))
    }
}