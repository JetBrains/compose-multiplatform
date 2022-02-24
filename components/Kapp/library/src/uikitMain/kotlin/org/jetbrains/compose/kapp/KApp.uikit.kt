package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.ui.window.*

import kotlinx.cinterop.*
import platform.UIKit.*
import platform.Foundation.*

class UIAppScope : FrameScope {
    // TODO: fix me
    override val density: Float
        get() = 1.0f

    override val widthPixels: Int
        get() = 800

    override val heightPixels: Int
        get() = 600
}

@Composable
actual fun KAppScope.Frame(content: @Composable FrameScope.() -> Unit) {
    val uiappScope = UIAppScope()
    uiappScope.apply {
        content()
    }
}

internal class AppAppScope : KAppScope {}

internal actual fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit) {
    // TODO: make it multiframe.
    val scope = AppAppScope()
    scope.apply {
        TODO()
    }
}

// TODO: an ugly hack - rework!
private var appName: String = ""
private var appContent: @Composable FrameScope.() -> Unit = {}

internal actual fun simpleKappImpl(name: String, content: @Composable FrameScope.() -> Unit) {
    val appScope = AppAppScope()

    appScope.apply {
        val args = emptyArray<String>()
        memScoped {
            val argc = args.size + 1
            val argv = (arrayOf(name) + args).map { it.cstr.ptr }.toCValues()
            appName = name
            appContent = content
            autoreleasepool {
                UIApplicationMain(argc, argv, null, NSStringFromClass(SkikoAppDelegate))
            }
        }

    }
}

internal class SkikoAppDelegate : UIResponder, UIApplicationDelegateProtocol {
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
        val frameScope = UIAppScope()
        frameScope.apply {
            window!!.rootViewController = Application(appName) {
                appContent()
            }
        }
        window!!.makeKeyAndVisible()
        return true
    }
}
