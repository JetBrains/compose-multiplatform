package org.jetbrains.compose

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.compose.experimental.internal.SUPPORTED_NATIVE_CACHE_KIND_PROPERTIES
import org.jetbrains.compose.internal.utils.BuildEventsListenerRegistryProvider
import org.jetbrains.compose.internal.utils.loadProperties
import org.jetbrains.compose.internal.utils.localPropertiesFile
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

// The service implements OperationCompletionListener just so Gradle would use the service
// even if the service is not used by any task or transformation
abstract class ComposeMultiplatformBuildService : BuildService<ComposeMultiplatformBuildService.Parameters>,
    OperationCompletionListener, AutoCloseable {

    abstract class Parameters : BuildServiceParameters {
        abstract val gradlePropertiesCacheKindSnapshot: MapProperty<String, String>
        abstract val localPropertiesCacheKindSnapshot: MapProperty<String, String>
    }

    private val log = Logging.getLogger(this.javaClass)

    internal abstract val unsupportedCompilerPlugins: SetProperty<Provider<SubpluginArtifact?>>
    internal abstract val delayedWarnings: SetProperty<String>
    internal val gradlePropertiesSnapshot: Map<String, String> = parameters.gradlePropertiesCacheKindSnapshot.get()
    internal val localPropertiesSnapshot: Map<String, String> = parameters.localPropertiesCacheKindSnapshot.get()

    fun warnOnceAfterBuild(message: String) {
        delayedWarnings.add(message)
    }

    override fun close() {
        notifyAboutUnsupportedCompilerPlugin()
        logDelayedWarnings()
    }

    private fun notifyAboutUnsupportedCompilerPlugin() {
        val unsupportedCompilerPlugin = unsupportedCompilerPlugins.orNull
            ?.firstOrNull()
            ?.orNull

        if (unsupportedCompilerPlugin != null) {
            log.error(createWarningAboutNonCompatibleCompiler(unsupportedCompilerPlugin.groupId))
        }
    }

    private fun logDelayedWarnings() {
        for (warning in delayedWarnings.get()) {
            log.warn(warning)
        }
    }

    override fun onFinish(event: FinishEvent) {}

    companion object {
        private val COMPOSE_SERVICE_FQ_NAME = ComposeMultiplatformBuildService::class.java.canonicalName

        private fun findExistingComposeService(project: Project): ComposeMultiplatformBuildService? {
            val registration = project.gradle.sharedServices.registrations.findByName(COMPOSE_SERVICE_FQ_NAME)
            val service = registration?.service?.orNull
            if (service != null) {
                if (service !is ComposeMultiplatformBuildService) {
                    // Compose Gradle plugin was probably loaded more than once
                    // See https://github.com/JetBrains/compose-multiplatform/issues/3459
                    if (service.javaClass.canonicalName == ComposeMultiplatformBuildService::class.java.canonicalName) {
                        val rootScript = project.rootProject.buildFile
                        error("""
                            Compose Multiplatform Gradle plugin has been loaded in multiple classloaders.
                            To avoid classloading issues, declare Compose Gradle Plugin in root build file $rootScript.
                        """.trimIndent())
                    } else {
                        error("Shared build service '$COMPOSE_SERVICE_FQ_NAME' has unexpected type: ${service.javaClass.canonicalName}")
                    }
                }
                return service
            }

            return null
        }

        fun getInstance(project: Project): ComposeMultiplatformBuildService =
            findExistingComposeService(project) ?: error("ComposeMultiplatformBuildService was not initialized!")

        @Suppress("UnstableApiUsage")
        fun init(project: Project) {
            val existingService = findExistingComposeService(project)
            if (existingService != null) {
                return
            }

            val newService = project.gradle.sharedServices.registerIfAbsent(COMPOSE_SERVICE_FQ_NAME, ComposeMultiplatformBuildService::class.java) {
                it.parameters.initPropertiesSnapshots(project.rootProject)
            }
            // workaround to instanciate a  service even if it not binded to a task
            BuildEventsListenerRegistryProvider.getInstance(project).onTaskCompletion(newService)
        }

        private fun Parameters.initPropertiesSnapshots(rootProject: Project) {
            // we want to record original properties (explicitly set by a user)
            // before we possibly change them in configureNativeCompilerCaching.kt
            val localProperties = loadProperties(rootProject.localPropertiesFile)
            for (cacheKindProperty in SUPPORTED_NATIVE_CACHE_KIND_PROPERTIES) {
                rootProject.findProperty(cacheKindProperty)?.toString()?.let { value ->
                    gradlePropertiesCacheKindSnapshot.put(cacheKindProperty, value)
                }
                localProperties[cacheKindProperty]?.toString()?.let { value ->
                    localPropertiesCacheKindSnapshot.put(cacheKindProperty, value)
                }
            }
        }
    }
}
