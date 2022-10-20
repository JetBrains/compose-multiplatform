package org.jetbrains.compose.experimental.uikit.tasks

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class DownloadXcodeGenTask : DefaultTask() {

    @get:Input
    internal abstract val downloadUrl: Property<String>

    @get:OutputFile
    internal abstract val destFile: RegularFileProperty

    @TaskAction
    fun run() {
        DownloadAction(project, this).also { action ->
            action.src(downloadUrl.get())
            action.dest(destFile.get().asFile)
        }.execute()
    }

}