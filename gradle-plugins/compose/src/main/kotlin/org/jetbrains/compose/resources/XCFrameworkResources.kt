package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.jetbrains.compose.internal.Version
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication

private const val MIN_KGP_VERSION_FOR_XCFRAMEWORK_RESOURCES = "2.2.0-Beta2-1"

@OptIn(ComposeKotlinGradlePluginApi::class)
internal fun Project.configureXCFrameworkComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    kgp: KotlinBasePlugin
) {
    val kgpVersion = Version.fromString(kgp.pluginVersion)
    val kmpResources = extraProperties.get(KMP_RES_EXT) as KotlinTargetResourcesPublication
    val requiredVersion = Version.fromString(MIN_KGP_VERSION_FOR_XCFRAMEWORK_RESOURCES)
    val isAvailable = kgpVersion >= requiredVersion

    tasks.withType(XCFrameworkTask::class.java).configureEach { task ->
        if (isAvailable) {
            logger.info("Configure compose resources in ${task.name}")
            kotlinExtension.targets
                .withType(KotlinNativeTarget::class.java)
                .configureEach { target ->
                    target.binaries.withType(Framework::class.java).configureEach { framework ->
                        task.addTargetResources(
                            resources = kmpResources.resolveResources(target),
                            target = framework.target.konanTarget
                        )
                    }
                }
        } else {
            logger.warn("Compose resources are supported in XCFrameworks " +
                    "since '$MIN_KGP_VERSION_FOR_XCFRAMEWORK_RESOURCES' Kotlin Gradle plugin version")
        }
    }
}