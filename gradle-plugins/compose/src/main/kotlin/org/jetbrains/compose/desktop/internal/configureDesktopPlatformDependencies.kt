package org.jetbrains.compose.desktop.internal

import org.gradle.api.Project
import org.gradle.api.attributes.AttributeContainer
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.kotlinJvmExt
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal fun Project.configureDesktopPlatformDependencies() {
    if (!ComposeProperties.disableJvmNativeAttributes(providers).get()) {
        val currentOs = DefaultNativePlatform.getCurrentOperatingSystem()
        val currentArch = DefaultNativePlatform.getCurrentArchitecture()

        val os = objects.named(
            OperatingSystemFamily::class.java, when {
                currentOs.isMacOsX -> OperatingSystemFamily.MACOS
                currentOs.isLinux -> OperatingSystemFamily.LINUX
                currentOs.isWindows -> OperatingSystemFamily.WINDOWS
                else -> "unknown"
            }
        )

        @Suppress("UnstableApiUsage") val arch = objects.named(
            MachineArchitecture::class.java, when {
                currentArch.isArm64 -> MachineArchitecture.ARM64
                currentArch.isAmd64 -> MachineArchitecture.X86_64
                else -> "unknown"
            }
        )

        plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
            mppExt.targets.withType(KotlinJvmTarget::class.java) {
                it.attributes.applyAttributes(os, arch)
            }
        }
        plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
            kotlinJvmExt.target.attributes.applyAttributes(os, arch)
        }
    }
}

private fun AttributeContainer.applyAttributes(os: OperatingSystemFamily?, arch: MachineArchitecture?) {
    os?.let { attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, os) }
    arch?.let { attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, arch) }
}
