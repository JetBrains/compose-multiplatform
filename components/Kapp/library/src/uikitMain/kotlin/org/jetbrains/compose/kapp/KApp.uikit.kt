package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.ui.window.*

import platform.UIKit.*
import platform.Foundation.*

@Composable
actual fun KAppScope.Frame(content: @Composable () -> Unit) {
    content()
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
private var appContent: @Composable () -> Unit = {}

internal actual fun simpleKappImpl(name: String, content: @Composable () -> Unit) {
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
        window!!.rootViewController = Application(appName) {
            appContent()
        }
        window!!.makeKeyAndVisible()
        return true
    }
}
