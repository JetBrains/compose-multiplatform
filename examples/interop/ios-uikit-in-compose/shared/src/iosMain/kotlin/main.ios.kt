import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

            var text: String by remember { mutableStateOf("This is iOS UITextField inside Compose") }
            ComposeUITextField(
                text,
                { text = it },
                Modifier.padding(4.dp).fillMaxWidth().height(30.dp).border(2.dp, Color.Blue)
            )
        }
    }
