package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication

@OptIn(ComposeKotlinGradlePluginApi::class)
internal fun Project.configureXCFrameworkComposeResources(
    kotlinExtension: KotlinMultiplatformExtension
) {
    val kmpResources = extraProperties.get(KMP_RES_EXT) as KotlinTargetResourcesPublication
    tasks.withType(XCFrameworkTask::class.java).configureEach { task ->
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
    }
}