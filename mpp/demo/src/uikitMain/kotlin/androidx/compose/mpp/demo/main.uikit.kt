// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import androidx.compose.runtime.remember
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.window.ComposeUIViewController

fun main() {
    defaultUIKitMain("ComposeDemo", ComposeUIViewController {
        val app = remember { App() }
        app.Content()
    })
}
