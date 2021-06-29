package org.jetbrains.compose.desktop.preview.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger as GradleLogger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.desktop.application.internal.javaExecutable
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.*
import java.io.File

abstract class AbstractConfigureDesktopPreviewTask : AbstractComposeDesktopTask() {
    @get:InputFiles
    internal lateinit var previewClasspath: FileCollection

    @get:Internal
    internal val javaHome: Property<String> = objects.notNullProperty<String>().apply {
        set(providers.systemProperty("java.home"))
    }

    // todo
    @get:Input
    @get:Optional
    internal val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Input
    internal val previewTarget: Provider<String> =
        project.providers.gradleProperty("compose.desktop.preview.target")

    @get:Input
    internal val idePort: Provider<String>  =
        project.providers.gradleProperty("compose.desktop.preview.ide.port")

    @get:InputFiles
    internal val hostClasspath = project.configurations.detachedConfiguration(
        project.dependencies.create("org.jetbrains.compose:preview-rpc:${ComposeBuildConfig.VERSION}")
    )

    @TaskAction
    fun run() {
        val hostConfig = PreviewHostConfig(
                javaExecutable = javaExecutable(javaHome.get()),
                hostClasspath = hostClasspath.files.pathString()
            )
        val previewClasspathString = previewClasspath.files.pathString()

        val gradleLogger = logger
        val previewLogger = GradlePreviewLoggerAdapter(gradleLogger)

        val connection = getLocalConnectionOrNull(idePort.get().toInt(), previewLogger, onClose = {})
        if (connection != null) {
            connection.use {
                connection.sendConfigFromGradle(
                    hostConfig,
                    previewClasspath = previewClasspathString,
                    previewFqName = previewTarget.get()
                )
            }
        } else {
            gradleLogger.error("Could not connect to IDE")
        }
    }

    private fun Collection<File>.pathString(): String =
        joinToString(File.pathSeparator) { it.absolutePath }

    private class GradlePreviewLoggerAdapter(
        private val logger: GradleLogger
    ) : PreviewLogger() {
        // todo: support compose.verbose
        override val isEnabled: Boolean
            get() = logger.isDebugEnabled

        override fun log(s: String) {
            logger.info("Compose Preview: $s")
        }
    }
}