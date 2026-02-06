package org.jetbrains.compose.desktop.internal

import org.gradle.api.Project
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.compose.desktop.application.internal.ComposeProperties

internal fun Project.configureDesktopPlatformDependencies() {
    if (!ComposeProperties.disableSkikoAwtRuntimeConstraints(providers).get()) {
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

        project.configurations.all { configuration ->
            if (!configuration.isCanBeConsumed || configuration.isCanBeResolved) {
                try {
                    configuration.dependencyConstraints.add(project.dependencies.constraints.create("org.jetbrains.skiko:skiko-awt-runtime") {
                        it.attributes { attrs ->
                            attrs.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, os)
                            attrs.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, arch)
                        }
                    })
                } catch (_: Throwable) {
                    project.logger.debug("Skipping os/arch constraint for config ${configuration.name}")
                }
            }
        }
    }
}
