package org.jetbrains.compose

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

// The service implements OperationCompletionListener just so Gradle would use the service
// even if the service is not used by any task or transformation
abstract class ComposeMultiplatformBuildService : BuildService<ComposeMultiplatformBuildService.Parameters>,
    OperationCompletionListener, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val unsupportedCompilerPlugins: SetProperty<Provider<SubpluginArtifact?>>
    }

    private val log = Logging.getLogger(this.javaClass)

    override fun close() {
        notifyAboutUnsupportedCompilerPlugin()
    }

    private fun notifyAboutUnsupportedCompilerPlugin() {
        val unsupportedCompilerPlugin = parameters.unsupportedCompilerPlugins.orNull
            ?.firstOrNull()
            ?.orNull

        if (unsupportedCompilerPlugin != null) {
            log.error(createWarningAboutNonCompatibleCompiler(unsupportedCompilerPlugin.groupId))
        }
    }

    override fun onFinish(event: FinishEvent) {}

    companion object {
        fun configure(project: Project, fn: Parameters.() -> Unit): Provider<ComposeMultiplatformBuildService> =
            project.gradle.sharedServices.registerOrConfigure<Parameters, ComposeMultiplatformBuildService> {
                fn()
            }

        fun provider(project: Project): Provider<ComposeMultiplatformBuildService> = configure(project) {}
    }
}

inline fun <reified P : BuildServiceParameters, reified S : BuildService<P>> BuildServiceRegistry.registerOrConfigure(
    crossinline fn: P.() -> Unit
): Provider<S> {
    val serviceClass = S::class.java
    val serviceFqName = serviceClass.canonicalName
    val existingService = registrations.findByName(serviceFqName)
        ?.apply { (parameters as? P)?.fn() }
        ?.service
    return (existingService as? Provider<S>)
        ?: registerIfAbsent(serviceFqName, serviceClass) {
            it.parameters.fn()
        }
}