/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

internal fun packageVersionFor(
    project: Project,
    app: JvmApplication,
    targetFormat: TargetFormat
): Provider<String?> =
    project.provider {
        app.nativeDistributions.packageVersionFor(targetFormat)
            ?: project.version.toString().takeIf { it != "unspecified" }
            ?: "1.0.0"
    }

private fun JvmApplicationDistributions.packageVersionFor(
    targetFormat: TargetFormat
): String? {
    val formatSpecificVersion: String? = when (targetFormat) {
        TargetFormat.AppImage -> null
        TargetFormat.Deb -> linux.debPackageVersion
        TargetFormat.Rpm -> linux.rpmPackageVersion
        TargetFormat.Dmg -> macOS.dmgPackageVersion
        TargetFormat.Pkg -> macOS.pkgPackageVersion
        TargetFormat.Exe -> windows.exePackageVersion
        TargetFormat.Msi -> windows.msiPackageVersion
    }
    val osSpecificVersion: String? = when (targetFormat.targetOS) {
        OS.Linux -> linux.packageVersion
        OS.MacOS -> macOS.packageVersion
        OS.Windows -> windows.packageVersion
    }
    return formatSpecificVersion
        ?: osSpecificVersion
        ?: packageVersion
}

internal fun packageBuildVersionFor(
    project: Project,
    app: JvmApplication,
    targetFormat: TargetFormat
): Provider<String?> =
    project.provider {
        app.nativeDistributions.packageBuildVersionFor(targetFormat)
            // fallback to normal version
            ?: app.nativeDistributions.packageVersionFor(targetFormat)
            ?: project.version.toString().takeIf { it != "unspecified" }
            ?: "1.0.0"
    }

private fun JvmApplicationDistributions.packageBuildVersionFor(
    targetFormat: TargetFormat
): String? {
    check(targetFormat.targetOS == OS.MacOS)
    val formatSpecificVersion: String? = when (targetFormat) {
        TargetFormat.AppImage -> null
        TargetFormat.Dmg -> macOS.dmgPackageBuildVersion
        TargetFormat.Pkg -> macOS.pkgPackageBuildVersion
        else -> error("invalid target format: $targetFormat")
    }
    val osSpecificVersion: String? = macOS.packageBuildVersion
    return formatSpecificVersion
        ?: osSpecificVersion
}
