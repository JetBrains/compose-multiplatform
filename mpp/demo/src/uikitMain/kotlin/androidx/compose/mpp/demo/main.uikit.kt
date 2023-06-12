// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import NativeModalWithNaviationExample
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
    val app = remember {
        App(
            extraScreens = listOf(
                NativeModalWithNaviationExample
            )
        )
    }
    app.Content()
}
