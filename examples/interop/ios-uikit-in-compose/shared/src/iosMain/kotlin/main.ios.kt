import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.MKMapView
import platform.UIKit.UIViewController

@OptIn(ExperimentalForeignApi::class)
fun ComposeEntryPoint(): UIViewController =
    ComposeUIViewController {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("How to use UIKitView inside Compose")

            BlueBorder {
                UIKitView(
                    factory = { MKMapView() },
                    modifier = Modifier.size(300.dp),
                    update = {},
                )
            }

            Spacer(Modifier.size(20.dp))

            BlueBorder {
                UseUITextField()
            }
        }
    }

/**
 * Border to easily understand the size of UIKitView
 */
@Composable
fun BlueBorder(content: @Composable () -> Unit) {
    Box(Modifier.padding(4.dp).border(2.dp, Color.Blue)) {
        content()
    }
}
