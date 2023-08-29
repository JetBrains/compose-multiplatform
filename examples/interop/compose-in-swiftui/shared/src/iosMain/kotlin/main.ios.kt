import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun ComposeOnly(): UIViewController =
    ComposeUIViewController {
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
                .background(Color.LightGray)
        ) {
            Text("top", Modifier.align(Alignment.TopCenter))
            Text("ComposeOnly", Modifier.align(Alignment.Center))
            Text("bottom", Modifier.align(Alignment.BottomCenter))
        }
    }
