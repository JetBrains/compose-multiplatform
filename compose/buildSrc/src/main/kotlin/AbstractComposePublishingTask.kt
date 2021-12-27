/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import org.gradle.api.*
import org.gradle.api.tasks.Internal

abstract class AbstractComposePublishingTask : DefaultTask() {
    @get:Internal
    lateinit var repository: String

    private val composeProperties by lazy {
        ComposeProperties(project)
    }
    private val isOelPublication: Boolean by lazy {
        composeProperties.isOelPublication
    }
    private val targetPlatforms: Set<ComposePlatforms> by lazy {
        composeProperties.targetPlatforms
    }

    abstract fun dependsOnComposeTask(task: String)

    fun publish(project: String, publications: Collection<String>) {
        for (publication in publications) {
            dependsOnComposeTask("$project:publish${publication}PublicationTo$repository")
        }
    }

    fun publish(project: String, publications: Collection<String>, onlyWithPlatforms: Set<ComposePlatforms>) {
        if (onlyWithPlatforms.any { it in targetPlatforms }) {
            publish(project, publications)
        }
    }

    fun publishMultiplatform(component: ComposeComponent) {
        dependsOnComposeTask("${component.path}:publish${ComposePlatforms.KotlinMultiplatform.name}PublicationTo$repository")

        for (platform in targetPlatforms) {
            if (platform !in component.supportedPlatforms) continue

            if (platform in ComposePlatforms.ANDROID && isOelPublication) continue

            dependsOnComposeTask("${component.path}:publish${platform.name}PublicationTo$repository")
        }
    }
}