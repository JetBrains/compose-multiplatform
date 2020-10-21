package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import java.io.File

abstract class PlatformSettings {
    var version: String? = null
    var installDir: String? = null
}

open class MacOSPlatformSettings : PlatformSettings() {
    var packageIdentifier: String? = null
    var packageName: String? = null
    val signing: MacOSSigningSettings = MacOSSigningSettings()

    private var isSignInitialized = false
    fun signing(fn: Action<MacOSSigningSettings>) {
        // enable sign if it the corresponding block is present in DSL
        if (!isSignInitialized) {
            isSignInitialized = true
            signing.sign = true
        }
        fn.execute(signing)
    }
}

open class MacOSSigningSettings {
    var sign: Boolean = false
    var keychain: File? = null
    var bundlePrefix: String? = null
    var keyUserName: String? = null
}

open class LinuxPlatformSettings : PlatformSettings() {
    var shortcut: Boolean = false
    var packageName: String? = null
    var appRelease: String? = null
    var appCategory: String? = null
    var debMaintainer: String? = null
    var menuGroup: String? = null
    var rpmLicenseType: String? = null
}

open class WindowsPlatformSettings : PlatformSettings() {
    var console: Boolean = false
    var dirChooser: Boolean = false
    var perUserInstall: Boolean = false
    var shortcut: Boolean = false
    var menu: Boolean = false
    var menuGroup: String? = null
    var upgradeUuid: String? = null
}