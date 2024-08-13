package org.jetbrains.compose.desktop.preview.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger as GradleLogger
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.desktop.ui.tooling.preview.rpc.*
import org.jetbrains.compose.internal.utils.*
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.internal.utils.javaExecutable
import org.jetbrains.compose.internal.utils.notNullProperty
import java.io.File

abstract class AbstractConfigureDesktopPreviewTask : AbstractComposeDesktopTask() {
    @get:InputFiles
    internal lateinit var previewClasspath: FileCollection

    @get:InputFiles
    internal abstract val skikoRuntime: Property<FileCollection>

    @get:Internal
    internal val javaHome: Property<String> = objects.notNullProperty<String>().apply {
        set(providers.systemProperty("java.home"))
    }

    // todo
    @get:Input
    @get:Optional
    internal val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Optional
    @get:Input
    internal val previewTarget: Provider<String> =
        project.providers.gradleProperty("compose.desktop.preview.target")

    @get:Optional
    @get:Input
    internal val idePort: Provider<String>  =
        project.providers.gradleProperty("compose.desktop.preview.ide.port")

    @get:InputFiles
    internal val uiTooling: FileCollection =
        project.detachedComposeDependency(
            groupId = "org.jetbrains.compose.ui",
            artifactId = "ui-tooling-desktop",
        ).excludeTransitiveDependencies()

    @get:InputFiles
    internal val hostClasspath: FileCollection =
        project.detachedComposeGradleDependency(artifactId = "preview-rpc")

    @TaskAction
    fun run() {
        val hostConfig = PreviewHostConfig(
                javaExecutable = javaExecutable(javaHome.get()),
                hostClasspath = hostClasspath.files.asSequence().pathString()
            )

        val skikoRuntimeFiles = skikoRuntime.get()
        val previewClasspathString =
            (previewClasspath.files.asSequence() +
                    uiTooling.files.asSequence() +
                    skikoRuntimeFiles.files.asSequence()
            ).pathString()

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

    internal fun tryGetSkikoRuntimeIfNeeded(): FileCollection {
        try {
            var hasSkikoJvm = false
            var hasSkikoJvmRuntime = false
            var skikoVersion: String? = null
            for (file in previewClasspath.files) {
                if (file.name.endsWith(".jar")) {
                    if (file.name.startsWith("skiko-awt-runtime-")) {
                        hasSkikoJvmRuntime = true
                        continue
                    } else if (file.name.startsWith("skiko-awt-")) {
                        hasSkikoJvm = true
                        skikoVersion = file.name
                            .removePrefix("skiko-awt-")
                            .removeSuffix(".jar")
                    }
                }
            }
            if (hasSkikoJvmRuntime) return project.files()

            if (hasSkikoJvm && !skikoVersion.isNullOrBlank()) {
                return project.detachedDependency(
                    groupId = "org.jetbrains.skiko",
                    artifactId = "skiko-awt-runtime-${currentTarget.id}",
                    version = skikoVersion
                ).excludeTransitiveDependencies()
            }
        } catch (e: Exception) {
            // OK
        }

        return project.files()
    }

    private fun Sequence<File>.pathString(): String =
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