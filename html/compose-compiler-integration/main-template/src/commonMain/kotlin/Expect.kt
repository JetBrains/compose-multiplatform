import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition

expect fun callComposable(content: @Composable () -> Unit)
