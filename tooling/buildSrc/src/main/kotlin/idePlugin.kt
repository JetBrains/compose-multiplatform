import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask

/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

fun Project.intellijPlugin(group: String, fn: IntelliJPluginExtension.() -> Unit = {}) {
    plugins.apply("org.jetbrains.intellij")

    val idePluginProperties = IdePluginBuildProperties(project)
    project.group = group
    project.version = idePluginProperties.deployVersion.get()

    val intellij = project.extensions.getByType(IntelliJPluginExtension::class.java)
    intellij.apply {
        pluginName.set(idePluginProperties.pluginName)
        type.set(idePluginProperties.platformType)
        version.set(idePluginProperties.platformVersion)
        downloadSources.set(idePluginProperties.platformDownloadSources)
        plugins.set(idePluginProperties.pluginDependencies)
    }

    fn(intellij)

    tasks.named("buildSearchableOptions").configure {
        // temporary workaround
        enabled = false
    }

    tasks.named<PublishPluginTask>("publishPlugin").configure {
        token.set(idePluginProperties.publishToken)
        channels.set(idePluginProperties.pluginChannels)
    }

    tasks.named<PatchPluginXmlTask>("patchPluginXml").configure {
        sinceBuild.set(idePluginProperties.pluginSinceBuild)
        untilBuild.set(idePluginProperties.pluginUntilBuild)
    }

    tasks.named<RunPluginVerifierTask>("runPluginVerifier").configure {
        ideVersions.set(idePluginProperties.pluginVerifierIdeVersions)
    }
}