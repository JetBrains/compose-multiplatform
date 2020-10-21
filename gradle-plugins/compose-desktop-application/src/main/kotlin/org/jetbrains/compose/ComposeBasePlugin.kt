package org.jetbrains.compose

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ComposeBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("compose", ComposeExtension::class.java)
    }
}