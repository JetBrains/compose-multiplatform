package org.jetbrains.compose

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
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
        const val skikoGroup = "org.jetbrains.skiko"

        private val majorMinorRegex = """^(\d+)\.(\d+)""".toRegex()
        fun majorMinorVersion(version: String): String {
            val match = majorMinorRegex.find(version) ?: return version
            val major = match.groupValues[1]
            val minor = match.groupValues[2]
            return "$major.$minor"
        }

        fun hasIncompatibleVersions(usages: List<DependencyUsage>): Boolean =
            usages
                .flatMap { usage -> listOfNotNull(usage.requestedVersion, usage.selectedVersion) }
                .map(::majorMinorVersion)
                .distinct()
                .size > 1
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
        val composeLibs = allDependencies.get()
            .mapNotNull { it.selected.moduleVersion }
            .filter { lib -> lib.group + ":" + lib.name in composeLibrariesForCheck }
            .distinctBy { lib -> "${lib.group}:${lib.name}:${lib.version}" }
        val composeProblems = composeLibs.mapNotNull { lib ->
            if (lib.version == expectedRuntimeVersion) return@mapNotNull null
            ProblemLibrary(lib.group + ":" + lib.name, lib.version)
        }
        if (composeProblems.isNotEmpty()) {
            logger.warn(
                getComposeMessage(
                    projectPath.get(),
                    configurationName.get(),
                    composeProblems,
                    expectedRuntimeVersion
                )
            )
        }

        val dependencyUsages = allDependencies.get().mapNotNull { dep ->
            val requested = dep.requested as? ModuleComponentSelector
            val selected = dep.selected.moduleVersion

            val requestedGroup = requested?.group
            val selectedGroup = selected?.group
            if (requestedGroup != skikoGroup && selectedGroup != skikoGroup) {
                return@mapNotNull null
            }

            DependencyUsage(
                requestedName = requested?.let { "${it.group}:${it.module}" },
                requestedVersion = requested?.version,
                selectedName = selected?.let { "${it.group}:${it.name}" },
                selectedVersion = selected?.version,
            )
        }.distinct()

        if (hasIncompatibleVersions(dependencyUsages)) {
            logger.warn(
                getSkikoMessage(
                    projectPath.get(),
                    configurationName.get(),
                    dependencyUsages
                )
            )
        }
    }

    private data class ProblemLibrary(val name: String, val version: String)
    private data class DependencyUsage(
        val requestedName: String?,
        val requestedVersion: String?,
        val selectedName: String?,
        val selectedVersion: String?,
    )

    private fun getComposeMessage(
        projectName: String,
        configurationName: String,
        problemLibs: List<ProblemLibrary>,
        expectedVersion: String,
    ): String = buildString {
        appendLine("w: Compose Multiplatform runtime dependencies' versions don't match with plugin version.")
        problemLibs.forEach { lib ->
            appendLine("    expected: '${lib.name}:$expectedVersion'")
            appendLine("    actual:   '${lib.name}:${lib.version}'")
            appendLine()
        }
        appendNoteAboutDependencyMismatch(projectName, configurationName)
        appendLine()
        appendLine("Please update Compose Multiplatform Gradle plugin's version or align dependencies' versions to match the current plugin version.")
    }

    private fun getSkikoMessage(
        projectName: String,
        configurationName: String,
        dependencyUsages: List<DependencyUsage>,
    ): String = buildString {
        appendLine("w: Skiko dependencies use incompatible versions in the dependency tree.")
        dependencyUsages.forEach { usage ->
                val requested = usage.requestedName?.let { "$it:${usage.requestedVersion}" } ?: "<unknown>"
                val selected = usage.selectedName?.let { "$it:${usage.selectedVersion}" } ?: "<unknown>"
                appendLine("    requested: '$requested'")
                appendLine("    selected:  '$selected'")
                appendLine()
            }
        appendNoteAboutDependencyMismatch(projectName, configurationName)
        appendLine()
        appendLine("Note: Skiko is considered implementation detail in Compose Multiplatform and might be incompatible across versions.")
        appendLine("Please align Skiko dependencies to the same version. If possible, avoid direct skiko references and use Compose APIs instead.")
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
}
