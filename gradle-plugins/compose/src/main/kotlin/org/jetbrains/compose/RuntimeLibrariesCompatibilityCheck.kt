package org.jetbrains.compose

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.KOTLIN_JVM_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.kotlinJvmExt
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.dependsOn
import org.jetbrains.compose.internal.utils.joinLowerCamelCase
import org.jetbrains.compose.internal.utils.provider
import org.jetbrains.compose.internal.utils.registerOrConfigure
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import javax.inject.Inject

internal fun Project.configureRuntimeLibrariesCompatibilityCheck() {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.configureEach { target -> target.configureRuntimeLibrariesCompatibilityCheck() }
    }
    plugins.withId(KOTLIN_JVM_PLUGIN_ID) {
        kotlinJvmExt.target.configureRuntimeLibrariesCompatibilityCheck()
    }
}

private fun KotlinTarget.configureRuntimeLibrariesCompatibilityCheck() {
    val target = this
    if (
        target.platformType == KotlinPlatformType.common ||
        target.platformType == KotlinPlatformType.androidJvm ||
        (target.platformType == KotlinPlatformType.jvm && target !is KotlinJvmTarget) //new AGP
    ) {
        return
    }
    compilations.configureEach { compilation ->
        val runtimeDependencyConfigurationName = if (target.platformType != KotlinPlatformType.native) {
            compilation.runtimeDependencyConfigurationName
        } else {
            compilation.compileDependencyConfigurationName
        } ?: return@configureEach
        val config = project.configurations.getByName(runtimeDependencyConfigurationName)

        val task = project.tasks.registerOrConfigure<RuntimeLibrariesCompatibilityCheck>(
            joinLowerCamelCase("check", target.name, compilation.name, "composeLibrariesCompatibility"),
        ) {
            expectedVersion.set(composeVersion)
            runtimeDependencies.set(provider { config.incoming.resolutionResult.allComponents })
        }
        compilation.compileTaskProvider.dependsOn(task)
    }
}

internal abstract class RuntimeLibrariesCompatibilityCheck : DefaultTask() {
    private companion object {
        const val FOUNDATION_DEP = "org.jetbrains.compose.foundation:foundation"
        const val UI_DEP = "org.jetbrains.compose.ui:ui"
    }

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Input
    abstract val expectedVersion: Property<String>

    @get:Input
    abstract val runtimeDependencies: SetProperty<ResolvedComponentResult>

    init {
        onlyIf {
            !ComposeProperties.disableLibraryCompatibilityCheck(providers).get()
        }
    }

    @TaskAction
    fun run() {
        val expectedRuntimeVersion = expectedVersion.get()
        val deps = listOf(FOUNDATION_DEP, UI_DEP)
        runtimeDependencies.get().forEach { component ->
            component.moduleVersion?.let { lib ->
                val depName = lib.group + ":" + lib.name
                if (depName in deps) {
                    val actualRuntimeVersion = lib.version
                    if (actualRuntimeVersion != expectedRuntimeVersion) {
                        logger.warn(
                            "w: runtime dependency version mismatch!\n\t" +
                                    "expected: '$depName:$expectedRuntimeVersion', " +
                                    "actual: '$depName:$actualRuntimeVersion'"
                        )
                    }
                }
            }
        }
    }
}