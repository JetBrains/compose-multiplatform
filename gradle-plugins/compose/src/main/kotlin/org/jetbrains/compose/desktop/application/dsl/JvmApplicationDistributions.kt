/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action

internal val DEFAULT_RUNTIME_MODULES = arrayOf(
    "java.base", "java.desktop", "java.logging", "jdk.crypto.ec"
)

abstract class JvmApplicationDistributions : AbstractDistributions() {
    var modules = arrayListOf(*DEFAULT_RUNTIME_MODULES)
    fun modules(vararg modules: String) {
        this.modules.addAll(modules.toList())
    }
    var includeAllModules: Boolean = false

    val linux: LinuxPlatformSettings = objects.newInstance(LinuxPlatformSettings::class.java)
    open fun linux(fn: Action<LinuxPlatformSettings>) {
        fn.execute(linux)
    }

    val macOS: JvmMacOSPlatformSettings = objects.newInstance(JvmMacOSPlatformSettings::class.java)
    open fun macOS(fn: Action<JvmMacOSPlatformSettings>) {
        fn.execute(macOS)
    }

    val windows: WindowsPlatformSettings = objects.newInstance(WindowsPlatformSettings::class.java)
    fun windows(fn: Action<WindowsPlatformSettings>) {
        fn.execute(windows)
    }
}