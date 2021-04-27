package org.jetbrains.compose.desktop.preview.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.internal.javaExecutable
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask

abstract class AbstractRunComposePreviewTask : AbstractComposeDesktopTask() {
    @get:InputFiles
    internal lateinit var classpath: FileCollection

    @get:Internal
    internal val javaHome: Property<String> = objects.notNullProperty<String>().apply {
        set(providers.systemProperty("java.home"))
    }

    @get:Input
    @get:Optional
    internal val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @TaskAction
    fun run() {
        val target = project.findProperty("compose.desktop.preview.target") as String
        execOperations.javaexec { javaExec ->
            javaExec.executable = javaExecutable(javaHome.get())
            javaExec.main = "androidx.compose.desktop.ui.tooling.preview.runtime.PreviewRunner"
            javaExec.classpath = classpath
            javaExec.args = listOf(target)
            javaExec.jvmArgs(jvmArgs.get())
        }
    }
}