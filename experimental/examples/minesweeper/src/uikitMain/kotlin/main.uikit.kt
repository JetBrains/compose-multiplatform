/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Use `xcodegen` first, then `open ./ComposeMinesweeper.xcodeproj` and then Run button in XCode.
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Application
import kotlinx.cinterop.*
import platform.UIKit.*
import platform.Foundation.*

fun main() {
    val args = emptyArray<String>()
    memScoped {
        val argc = args.size + 1
        val argv = (arrayOf("skikoApp") + args).map { it.cstr.ptr }.toCValues()
        autoreleasepool {
            UIApplicationMain(argc, argv, null, NSStringFromClass(SkikoAppDelegate))
        }
    }
}

class SkikoAppDelegate : UIResponder, UIApplicationDelegateProtocol {
    companion object : UIResponderMeta(), UIApplicationDelegateProtocolMeta

    @ObjCObjectBase.OverrideInit
    constructor() : super()

    private var _window: UIWindow? = null
    override fun window() = _window
    override fun setWindow(window: UIWindow?) {
        _window = window
    }

    override fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean {
        window = UIWindow(frame = UIScreen.mainScreen.bounds)
        window!!.rootViewController = Application("Minesweeper") {
            Column {
                // To skip upper part of screen.
                Box(modifier = Modifier
                    .height(100.dp))
                Game()
            }
        }
        window!!.makeKeyAndVisible()
        return true
    }
}

@Composable
actual fun loadImage(src: String): Painter {
    // TODO Bundle pics and show images properly
    val color = when (src) {
        "assets/clock.png" -> Color.Blue
        "assets/flag.png" -> Color.Green
        "assets/mine.png" -> Color.Red
        else -> Color.White
    }

    return object : Painter() {
        override val intrinsicSize: Size
            get() = Size(16f, 16f)

        override fun DrawScope.onDraw() {
            drawRect(color = color)
        }
    }
}