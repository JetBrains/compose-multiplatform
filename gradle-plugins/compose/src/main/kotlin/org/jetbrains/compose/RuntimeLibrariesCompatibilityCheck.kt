package org.jetbrains.compose

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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
            allDependencies.set(
                provider {
                    config.incoming.resolutionResult.allDependencies
                        .filterIsInstance<ResolvedDependencyResult>()
                }
            )
        }
        compilation.compileTaskProvider.dependsOn(task)
    }
}

internal abstract class RuntimeLibrariesCompatibilityCheck : DefaultTask() {
    private companion object {
        val composeLibrariesForCheck = setOf(
            "org.jetbrains.compose.foundation:foundation",
            "org.jetbrains.compose.ui:ui"
        )
        val skikoLibraryForCheck = "org.jetbrains.skiko:skiko"

        private val majorMinorRegex = """^(\d+)\.(\d+)""".toRegex()
        fun majorMinorVersion(version: String): String {
            val match = majorMinorRegex.find(version) ?: return version
            val major = match.groupValues[1]
            val minor = match.groupValues[2]
            return "$major.$minor"
        }
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
    abstract val allDependencies: SetProperty<ResolvedDependencyResult>

    init {
        onlyIf {
            !ComposeProperties.disableLibraryCompatibilityCheck(providers).get()
        }
    }

    @TaskAction
    fun run() {
        val expectedRuntimeVersion = expectedVersion.get()
        val composeLibraries = allDependencies.get()
            .mapNotNull { it.selected.moduleVersion }
            .filter { lib -> "${lib.group}:${lib.name}" in composeLibrariesForCheck }
            .distinctBy { lib -> "${lib.group}:${lib.name}:${lib.version}" }
        val composeInconsistentVersions = composeLibraries.filter { lib ->
            lib.version != expectedRuntimeVersion
        }
        if (composeInconsistentVersions.isNotEmpty()) {
            logger.warn(
                getComposeMessage(
                    projectPath.get(),
                    configurationName.get(),
                    composeInconsistentVersions,
                    expectedRuntimeVersion
                )
            )
        }

        val skikoIncompatibleDependencyUsages = allDependencies.get().filter { dependency ->
            val requested = dependency.requested as? ModuleComponentSelector ?: return@filter false
            val selected = dependency.selected.moduleVersion ?: return@filter false
            if ("${requested.group}:${requested.module}" != skikoLibraryForCheck) return@filter false
            if ("${selected.group}:${selected.name}" != skikoLibraryForCheck) return@filter false
            majorMinorVersion(requested.version) != majorMinorVersion(selected.version)
        }
        if (skikoIncompatibleDependencyUsages.isNotEmpty()) {
            logger.warn(
                getSkikoMessage(
                    projectPath.get(),
                    configurationName.get(),
                    skikoIncompatibleDependencyUsages
                )
            )
        }
    }

    private fun getComposeMessage(
        projectName: String,
        configurationName: String,
        composeInconsistentVersions: List<ModuleVersionIdentifier>,
        expectedVersion: String,
    ): String = buildString {
        appendLine("w: Compose Multiplatform runtime dependencies' versions don't match with plugin version.")
        composeInconsistentVersions.forEach { library ->
            appendLine("    expected: '${library.group}:${library.name}:$expectedVersion'")
            appendLine("    actual:   '${library.group}:${library.name}:${library.version}'")
            appendLine()
        }
        appendNoteAboutDependencyMismatch(projectName, configurationName)
        appendLine()
        appendLine("Please update Compose Multiplatform Gradle plugin's version or align dependencies' versions to match the current plugin version.")
    }

    private fun getSkikoMessage(
        projectName: String,
        configurationName: String,
        dependencyUsages: List<ResolvedDependencyResult>,
    ): String = buildString {
        appendLine("w: Skiko dependencies' versions are incompatible.")
        dependencyUsages.forEach { usage ->
            val from = usage.from.moduleVersion
            val requested = usage.requested
            val selected = usage.selected.moduleVersion
            appendLine("    ${from.toModuleString()}")
            appendLine("    \\--- ${requested.toModuleString()} -> ${selected?.version}")
            appendLine()
        }
        appendNoteAboutDependencyMismatch(projectName, configurationName)
        appendLine()
        appendLine("Note: Skiko is considered implementation detail in Compose Multiplatform and might be incompatible across versions.")
        appendLine("Please align Skiko dependencies to the same version. If possible, avoid direct Skiko references and use Compose APIs instead.")
    }

    private fun StringBuilder.appendNoteAboutDependencyMismatch(
        projectName: String,
        configurationName: String,
    ) {
        appendLine("This may lead to compilation errors or unexpected behavior at runtime.")
        appendLine("Such version mismatch might be caused by dependency constraints in one of the included libraries.")
        val taskName = if (projectName.isNotEmpty() && !projectName.endsWith(":")) "$projectName:dependencies" else "${projectName}dependencies"
        appendLine("You can inspect resulted dependencies tree via `./gradlew $taskName  --configuration ${configurationName}`.")
        appendLine("See more details in Gradle documentation: https://docs.gradle.org/current/userguide/viewing_debugging_dependencies.html#sec:listing-dependencies")
    }

    private fun ModuleVersionIdentifier?.toModuleString() = when (this) {
        null -> "<unknown>"
        else -> "$group:$name:$version"
    }
    private fun ComponentSelector?.toModuleString(): String = when (this) {
        is ProjectComponentSelector -> "project $projectPath"
        is ModuleComponentSelector -> "$group:$module:$version"
        null -> "<unknown>"
        else -> displayName
    }
}
