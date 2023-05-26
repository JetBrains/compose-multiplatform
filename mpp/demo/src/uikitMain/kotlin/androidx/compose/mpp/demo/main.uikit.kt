// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import ApplicationLayoutExamples
import androidx.compose.runtime.*
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.window.ComposeUIViewController

fun main() {
    defaultUIKitMain("ComposeDemo", ComposeUIViewController {
        IosDemo()
    })
}

@Composable
fun IosDemo() {
    // You may uncomment different examples:
//    MultiplatformDemo()
    ApplicationLayoutExamples()
}

@Composable
fun MultiplatformDemo() {
    val app = remember { App() }
    app.Content()
}
