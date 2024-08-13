import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun ComposeEntryPoint(): UIViewController =
    ComposeUIViewController {
        ComposeApp()
    }

@Composable
private fun ComposeApp() { // This function also may be placed in commonMain source set.
    MaterialTheme(colors = if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        Surface {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .background(Color.Green.copy(alpha = 0.3f))
            ) {
                Text("top", Modifier.align(Alignment.TopCenter))
                Text("ComposeApp", Modifier.align(Alignment.Center))
                Text("bottom", Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
