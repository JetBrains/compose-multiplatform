package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class PlatformSettings (objects: ObjectFactory) {
    val iconFile: RegularFileProperty = objects.fileProperty()
}

open class MacOSPlatformSettings @Inject constructor(objects: ObjectFactory): PlatformSettings(objects) {
    var packageName: String? = null

    /**
     * An application's unique identifier across Apple's ecosystem.
     *
     * May only contain alphanumeric characters (A-Z,a-z,0-9), hyphen (-) and period (.) characters
     *
     * Use of a reverse DNS notation (e.g. com.mycompany.myapp) is recommended.
     */
    var bundleID: String? = null

    internal var signingSettings: MacOSSigningSettings? = null
    fun signing(fn: Action<MacOSSigningSettings>) {
        check(signingSettings == null) { "Signing is already configured" }
        signingSettings = MacOSSigningSettings().also {
            fn.execute(it)
            checkNotNull(it.identity)
        }
    }

    internal var notarizationSettings: MacOSNotarizationSettings? = null
    fun notarization(fn: Action<MacOSNotarizationSettings>) {
        check(notarizationSettings == null) { "Notarization is already configured" }
        notarizationSettings = MacOSNotarizationSettings().also {
            fn.execute(it)
            checkNotNull(it.appleID)
            checkNotNull(it.password)
        }
    }
}

open class MacOSSigningSettings {
    @get:Input
    lateinit var identity: String

    @get:Input
    @get:Optional
    var keychain: String? = null

    @get:Input
    @get:Optional
    var signPrefix: String? = null
}

open class MacOSNotarizationSettings {
    @get:Input
    lateinit var appleID: String

    @get:Input
    lateinit var password: String
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