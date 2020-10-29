package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import java.io.File
import javax.inject.Inject

abstract class PlatformSettings (objects: ObjectFactory) {
    val iconFile: RegularFileProperty = objects.fileProperty()
}

open class MacOSPlatformSettings @Inject constructor(objects: ObjectFactory): PlatformSettings(objects) {
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

open class LinuxPlatformSettings @Inject constructor(objects: ObjectFactory): PlatformSettings(objects) {
    var shortcut: Boolean = false
    var packageName: String? = null
    var appRelease: String? = null
    var appCategory: String? = null
    var debMaintainer: String? = null
    var menuGroup: String? = null
    var rpmLicenseType: String? = null
}

open class WindowsPlatformSettings @Inject constructor(objects: ObjectFactory): PlatformSettings(objects) {
    var console: Boolean = false
    var dirChooser: Boolean = true
    var perUserInstall: Boolean = false
    var shortcut: Boolean = false
    var menu: Boolean = false
        get() = field || menuGroup != null
    var menuGroup: String? = null
    var upgradeUuid: String? = null
}