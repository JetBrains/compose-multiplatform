package org.jetbrains.compose.desktop.preview.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.desktop.application.internal.javaExecutable
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import java.io.File
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket

abstract class AbstractConfigureDesktopPreviewTask : AbstractComposeDesktopTask() {
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
        val port = project.findProperty("compose.desktop.preview.port") as String
        val serverCP = project.configurations.detachedConfiguration(project.dependencies.create("org.jetbrains.compose:compose-desktop-preview-server:${ComposeBuildConfig.VERSION}")).files

        val s = Socket(InetAddress.getByName("127.0.0.1"), port.toInt())
        s.getOutputStream().buffered().use {
            PrintWriter(it).use { pw ->
                pw.println(javaExecutable(javaHome.get()))
                pw.println(serverCP.toList().joinToString(File.pathSeparator) { it.absolutePath })
                pw.println(target)
                pw.println(classpath.toList().joinToString(File.pathSeparator) { it.absolutePath })
            }
        }
    }
}