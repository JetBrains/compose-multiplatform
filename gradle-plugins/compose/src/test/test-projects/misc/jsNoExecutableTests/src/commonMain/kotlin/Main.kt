
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ReversedTextView(text: String, modifier: Modifier = Modifier) {
    BasicText(text.reversed(), modifier)
}