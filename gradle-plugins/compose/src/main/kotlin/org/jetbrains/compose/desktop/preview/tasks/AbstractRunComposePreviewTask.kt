package org.jetbrains.compose.desktop.preview.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.internal.javaExecutable
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import org.jetbrains.compose.desktop.preview.internal.PREVIEW_RUNTIME_CLASSPATH_CONFIGURATION
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask

abstract class AbstractRunComposePreviewTask : AbstractComposeDesktopTask() {
    @get:InputFiles
    internal lateinit var classpath: FileCollection

    @get:InputFiles
    internal val previewRuntimeClasspath: FileCollection
        get() = project.configurations.getByName(PREVIEW_RUNTIME_CLASSPATH_CONFIGURATION)

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
            javaExec.main = "org.jetbrains.compose.desktop.preview.runtime.ComposePreviewRunner"
            javaExec.classpath = previewRuntimeClasspath + classpath
            javaExec.args = listOf(target)
        }
    }
}