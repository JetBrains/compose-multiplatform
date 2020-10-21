package org.jetbrains.compose.desktop

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.ComposeBasePlugin
import org.jetbrains.compose.ComposeExtension

open class DesktopBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(ComposeBasePlugin::class.java)
        val composeExt = project.extensions.getByType(ComposeExtension::class.java)
        composeExt.extensions.create("desktop", DesktopExtension::class.java)
    }
}