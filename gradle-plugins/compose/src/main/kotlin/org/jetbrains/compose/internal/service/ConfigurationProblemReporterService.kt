/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.service

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.compose.createWarningAboutNonCompatibleCompiler
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact

abstract class ConfigurationProblemReporterService : AbstractComposeMultiplatformBuildService<ConfigurationProblemReporterService.Parameters>() {
    interface Parameters : BuildServiceParameters {
        val unsupportedPluginWarningProviders: ListProperty<Provider<String?>>
        val warnings: SetProperty<String>
    }

    private val log = Logging.getLogger(this.javaClass)

    override fun close() {
        warnAboutUnsupportedCompilerPlugin()
        logWarnings()
    }

    private fun warnAboutUnsupportedCompilerPlugin() {
        for (warningProvider in parameters.unsupportedPluginWarningProviders.get()) {
            val warning = warningProvider.orNull
            if (warning != null) {
                log.warn(warning)
            }
        }
    }

    private fun logWarnings() {
        for (warning in parameters.warnings.get()) {
            log.warn(warning)
        }
    }
    companion object {
        fun init(project: Project) {
            registerServiceIfAbsent<ConfigurationProblemReporterService, Parameters>(project)
        }

        private inline fun configureParameters(project: Project, fn: Parameters.() -> Unit) {
            getExistingServiceRegistration<ConfigurationProblemReporterService, Parameters>(project)
                .parameters.fn()
        }

        fun reportWarning(project: Project, message: String) {
            configureParameters(project) { warnings.add(message) }
        }

        fun registerUnsupportedPluginProvider(project: Project, unsupportedPlugin: Provider<SubpluginArtifact?>) {
            configureParameters(project) {
                unsupportedPluginWarningProviders.add(unsupportedPlugin.map { unsupportedCompiler ->
                    unsupportedCompiler?.groupId?.let { createWarningAboutNonCompatibleCompiler(it) }
                })
            }
        }
    }
}