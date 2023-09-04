import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
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
            UIKitView(
                factory = { MKMapView() },
                modifier = Modifier.size(300.dp).border(2.dp, Color.Blue),
                update = {},
            )

            Spacer(Modifier.size(20.dp))

            UseUITextField()
        }
    }
