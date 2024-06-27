import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.WindowInsets
import androidx.compose.ui.window.systemBars
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
            Text("How to use UIKitView inside Compose Multiplatform")

            UIKitView(
                factory = { MKMapView() },
                modifier = Modifier.padding(4.dp).border(2.dp, Color.Blue).size(300.dp),
                update = {},
            )

            Spacer(Modifier.size(20.dp))

            UseUITextField(Modifier.padding(4.dp).border(2.dp, Color.Blue))
        }
    }
