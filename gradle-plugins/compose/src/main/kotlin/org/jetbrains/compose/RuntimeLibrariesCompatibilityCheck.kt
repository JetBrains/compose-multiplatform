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
            projectPath.set(project.path)
            configurationName.set(runtimeDependencyConfigurationName)
            runtimeDependencies.set(provider { config.incoming.resolutionResult.allComponents })
        }
        compilation.compileTaskProvider.dependsOn(task)
    }
}

internal abstract class RuntimeLibrariesCompatibilityCheck : DefaultTask() {
    private companion object {
        val librariesForCheck = listOf(
            "org.jetbrains.compose.foundation:foundation",
            "org.jetbrains.compose.ui:ui"
        )
    }

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Input
    abstract val expectedVersion: Property<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val configurationName: Property<String>

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
        val foundLibs = runtimeDependencies.get().filter { component ->
            component.moduleVersion?.let { lib -> lib.group + ":" + lib.name } in librariesForCheck
        }
        val problems = foundLibs.mapNotNull { component ->
            val module = component.moduleVersion ?: return@mapNotNull null
            if (module.version == expectedRuntimeVersion) return@mapNotNull null
            ProblemLibrary(module.group + ":" + module.name, module.version)
        }

        if (problems.isNotEmpty()) {
            logger.warn(
                getMessage(
                    projectPath.get(),
                    configurationName.get(),
                    problems,
                    expectedRuntimeVersion
                )
            )
        }
    }

    private data class ProblemLibrary(val name: String, val version: String)

    private fun getMessage(
        projectName: String,
        configurationName: String,
        problemLibs: List<ProblemLibrary>,
        expectedVersion: String
    ): String = buildString {
        appendLine("w: Compose Multiplatform runtime dependencies' versions don't match with plugin version.")
        problemLibs.forEach { lib ->
            appendLine("    expected: '${lib.name}:$expectedVersion'")
            appendLine("    actual:   '${lib.name}:${lib.version}'")
            appendLine()
        }
        appendLine("This may lead to compilation errors or unexpected behavior at runtime.")
        appendLine("Such version mismatch might be caused by dependency constraints in one of the included libraries.")
        val taskName = if (projectName.isNotEmpty() && !projectName.endsWith(":")) "$projectName:dependencies" else "${projectName}dependencies"
        appendLine("You can inspect resulted dependencies tree via `./gradlew $taskName  --configuration ${configurationName}`.")
        appendLine("See more details in Gradle documentation: https://docs.gradle.org/current/userguide/viewing_debugging_dependencies.html#sec:listing-dependencies")
        appendLine()
        appendLine("Please update Compose Multiplatform Gradle plugin's version or align dependencies' versions to match the current plugin version.")
    }
}