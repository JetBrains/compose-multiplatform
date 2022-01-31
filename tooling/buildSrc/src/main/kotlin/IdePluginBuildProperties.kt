/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.provider.Provider

class IdePluginBuildProperties(private val project: Project) {
    val deployVersion get() = gradleProperty("deploy.version")
    val pluginName get() = gradleProperty("plugin.name")
    val pluginDependencies get() = gradleProperty("plugin.dependencies").commaSeparatedList()
    val platformType get() = gradleProperty("platform.type")
    val platformVersion get() = gradleProperty("platform.version")
    val platformDownloadSources get() = gradleProperty("platform.download.sources").toBoolean()
    val pluginChannels get() = gradleProperty("plugin.channels").commaSeparatedList()
    val pluginSinceBuild get() = gradleProperty("plugin.since.build")
    val pluginUntilBuild get() = gradleProperty("plugin.until.build")
    val pluginVerifierIdeVersions get() = gradleProperty("plugin.verifier.ide.versions").commaSeparatedList()
    val publishToken get() = envVar("IDE_PLUGIN_PUBLISH_TOKEN")

    private fun envVar(key: String): Provider<String> =
        project.providers.environmentVariable(key)

    private fun gradleProperty(key: String): Provider<String> =
        project.provider {
            (project.findProperty(key) as? Any)?.toString()
                ?: error("$project does not specify '$key' property")
        }

    private fun Provider<String>.toBoolean(): Provider<Boolean> =
        map { it.toBoolean() }

    private fun Provider<String>.commaSeparatedList(): Provider<List<String>> =
        map { str -> str.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
}