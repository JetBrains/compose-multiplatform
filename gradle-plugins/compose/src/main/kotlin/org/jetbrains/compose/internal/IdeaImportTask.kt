package org.jetbrains.compose.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

internal fun Project.ideaIsInSyncProvider(): Provider<Boolean> = provider {
    System.getProperty("idea.sync.active", "false").toBoolean()
}

/**
 * This task should be FAST and SAFE! Because it is being run during IDE import.
 */
internal abstract class IdeaImportTask : DefaultTask() {
    @get:Input
    val ideaIsInSync: Provider<Boolean> = project.ideaIsInSyncProvider()

    @TaskAction
    fun run() {
        try {
            safeAction()
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: $name task was failed:", e)
            if (!ideaIsInSync.get()) throw e
        }
    }

    abstract fun safeAction()
}