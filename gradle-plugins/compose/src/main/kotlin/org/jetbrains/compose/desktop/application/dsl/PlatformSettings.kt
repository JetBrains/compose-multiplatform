/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class PlatformSettings (objects: ObjectFactory) {
    val iconFile: RegularFileProperty = objects.fileProperty()
    var packageVersion: String? = null
    var installationPath: String? = null
}

open class MacOSPlatformSettings @Inject constructor(objects: ObjectFactory): PlatformSettings(objects) {
    var packageName: String? = null
    var dockName: String? = null
    var setDockNameSameAsPackageName: Boolean = true
    var appStore: Boolean = false
    var appCategory: String? = null
    var entitlementsFile: RegularFileProperty = objects.fileProperty()
    var runtimeEntitlementsFile: RegularFileProperty = objects.fileProperty()
    var packageBuildVersion: String? = null
    var dmgPackageVersion: String? = null
    var dmgPackageBuildVersion: String? = null
    var pkgPackageVersion: String? = null
    var pkgPackageBuildVersion: String? = null

    /**
     * An application's unique identifier across Apple's ecosystem.
     *
     * May only contain alphanumeric characters (A-Z,a-z,0-9), hyphen (-) and period (.) characters
     *
     * Use of a reverse DNS notation (e.g. com.mycompany.myapp) is recommended.
     */
    var bundleID: String? = null

    val signing: MacOSSigningSettings = objects.newInstance(MacOSSigningSettings::class.java)
    fun signing(fn: Action<MacOSSigningSettings>) {
        fn.execute(signing)
    }

    val notarization: MacOSNotarizationSettings = objects.newInstance(MacOSNotarizationSettings::class.java)
    fun notarization(fn: Action<MacOSNotarizationSettings>) {
        fn.execute(notarization)
    }

    val provisioningProfile: RegularFileProperty = objects.fileProperty()
    val runtimeProvisioningProfile: RegularFileProperty = objects.fileProperty()

    internal val infoPlistSettings = InfoPlistSettings()
    fun infoPlist(fn: Action<InfoPlistSettings>) {
        fn.execute(infoPlistSettings)
    }
}

open class InfoPlistSettings {
    var extraKeysRawXml: String? = null
}

open class LinuxPlatformSettings @Inject constructor(objects: ObjectFactory): PlatformSettings(objects) {
    var shortcut: Boolean = false
    var packageName: String? = null
    var appRelease: String? = null
    var appCategory: String? = null
    var debMaintainer: String? = null
    var menuGroup: String? = null
    var rpmLicenseType: String? = null
    var debPackageVersion: String? = null
    var rpmPackageVersion: String? = null
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
    var msiPackageVersion: String? = null
    var exePackageVersion: String? = null
}