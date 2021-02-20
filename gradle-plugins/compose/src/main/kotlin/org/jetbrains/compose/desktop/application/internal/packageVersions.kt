package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.compose.desktop.application.dsl.Application
import org.jetbrains.compose.desktop.application.dsl.NativeDistributions
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

internal fun packageVersionFor(
    project: Project,
    app: Application,
    targetFormat: TargetFormat
): Provider<String?> =
    project.provider {
        app.nativeDistributions.packageVersionFor(targetFormat)
            ?: project.version.toString().takeIf { it != "unspecified" }
    }

private fun NativeDistributions.packageVersionFor(
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
