package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.desktop.application.dsl.Application
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.util.*

// todo: multiple launchers
// todo: file associations
// todo: use workers
fun configureApplicationImpl(project: Project, app: Application) {
    project.afterEvaluate {
        if (app._isDefaultConfigurationEnabled) {
            if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                project.configureFromMppPlugin(app)
            } else if (project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                val mainSourceSet = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.getByName("main")
                app.from(mainSourceSet)
            }
        }
        project.configurePackagingTasks(listOf(app))
        project.configureWix()
    }
}

internal fun Project.configureFromMppPlugin(mainApplication: Application) {
    val kotlinExt = extensions.getByType(KotlinMultiplatformExtension::class.java)
    var isJvmTargetConfigured = false
    kotlinExt.targets.all { target ->
        if (target.platformType == KotlinPlatformType.jvm) {
            if (!isJvmTargetConfigured) {
                mainApplication.from(target)
                isJvmTargetConfigured = true
            } else {
                logger.error("w: Default configuration for Compose Desktop Application is disabled: " +
                        "multiple Kotlin JVM targets definitions are detected. " +
                        "Specify, which target to use by using `compose.desktop.application.from(kotlinMppTarget)`")
                mainApplication.disableDefaultConfiguration()
            }
        }
    }
}

internal fun Project.configurePackagingTasks(apps: Collection<Application>) {
    for (app in apps) {
        configureRunTask(app)
        configurePackagingTasks(app)
    }
}

internal fun Project.configurePackagingTasks(app: Application): TaskProvider<DefaultTask> {
    val packageFormats = app.nativeDistributions.targetFormats.map { targetFormat ->
        tasks.composeTask<AbstractJPackageTask>(
            taskName("package", app, targetFormat.name),
            args = listOf(targetFormat)
        ) {
            configurePackagingTask(app)
        }
    }
    return tasks.composeTask<DefaultTask>(taskName("package", app)) {
        dependsOn(packageFormats)
    }
}

internal fun AbstractJPackageTask.configurePackagingTask(app: Application) {
    enabled = targetFormat.isCompatibleWithCurrentOS

    if (targetFormat != TargetFormat.AppImage) {
        configurePlatformSettings(app)
    }

    app.nativeDistributions.let { executables ->
        packageName.set(provider { executables.packageName ?: project.name })
        packageDescription.set(provider { executables.description })
        packageCopyright.set(provider { executables.copyright })
        packageVendor.set(provider { executables.vendor })
        packageVersion.set(provider {
                executables.version
                    ?: project.version.toString().takeIf { it != "unspecified" }
        })
    }

    destinationDir.set(app.nativeDistributions.outputBaseDir.map { it.dir("${app.name}/${targetFormat.id}") })
    javaHome.set(provider { app.javaHomeOrDefault() })

    launcherMainJar.set(app.mainJar.orNull)
    app._fromFiles.forEach { files.from(it) }
    dependsOn(*app._dependenciesTaskNames.toTypedArray())

    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        files.from(configSource.runtimeClasspath)
        launcherMainJar.set(app.mainJar.orElse(configSource.jarTask(project).flatMap { it.archiveFile }))
    }

    modules.set(provider { app.nativeDistributions.modules })
    launcherMainClass.set(provider { app.mainClass })
    launcherJvmArgs.set(provider { app.jvmArgs })
    launcherArgs.set(provider { app.args })
}

internal fun AbstractJPackageTask.configurePlatformSettings(app: Application) {
    when (currentOS) {
        OS.Linux -> {
            app.nativeDistributions.linux.also { linux ->
                linuxShortcut.set(provider { linux.shortcut })
                linuxAppCategory.set(provider { linux.appCategory })
                linuxAppRelease.set(provider { linux.appRelease })
                linuxDebMaintainer.set(provider { linux.debMaintainer })
                linuxMenuGroup.set(provider { linux.menuGroup })
                linuxPackageName.set(provider { linux.packageName })
                linuxRpmLicenseType.set(provider { linux.rpmLicenseType })
                iconFile.set(linux.iconFile)
            }
        }
        OS.Windows -> {
            app.nativeDistributions.windows.also { win ->
                winConsole.set(provider { win.console })
                winDirChooser.set(provider { win.dirChooser })
                winPerUserInstall.set(provider { win.perUserInstall })
                winShortcut.set(provider { win.shortcut })
                winMenu.set(provider { win.menu })
                winMenuGroup.set(provider { win.menuGroup })
                winUpgradeUuid.set(provider { win.upgradeUuid })
                iconFile.set(win.iconFile)
            }
        }
        OS.MacOS -> {
            app.nativeDistributions.macOS.also { mac ->
                macPackageName.set(provider { mac.packageName })
                macPackageIdentifier.set(provider { mac.packageIdentifier })
                macSign.set(provider { mac.signing.sign })
                macSigningKeyUserName.set(provider { mac.signing.keyUserName })
                macSigningKeychain.set(project.layout.file(provider { mac.signing.keychain }))
                macBundleSigningPrefix.set(provider { mac.signing.bundlePrefix })
                iconFile.set(mac.iconFile)
            }
        }
    }
}

private fun Project.configureRunTask(app: Application) {
    project.tasks.composeTask<JavaExec>(taskName("run", app)) {
        mainClass.set(provider { app.mainClass })
        executable = javaExecutable(app.javaHomeOrDefault())
        jvmArgs = app.jvmArgs
        args = app.args

        val cp = objects.fileCollection()
        cp.from(app.mainJar.orNull)
        cp.from(app._fromFiles)
        dependsOn(*app._dependenciesTaskNames.toTypedArray())

        app._configurationSource?.let { configSource ->
            dependsOn(configSource.jarTaskName)
            cp.from(configSource.runtimeClasspath)
        }

        classpath = cp
    }
}

private fun Application.javaHomeOrDefault(): String =
    javaHome ?: System.getProperty("java.home")

private fun javaExecutable(javaHome: String): String {
    val executableName = if (currentOS == OS.Windows) "java.exe" else "java"
    return File(javaHome).resolve("bin/$executableName").absolutePath
}

private inline fun <reified T : Task> TaskContainer.composeTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline configureFn: T.() -> Unit = {}
) = register(name, T::class.java, *args.toTypedArray()).apply {
    configure {
        it.group = "compose desktop"
        it.configureFn()
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun taskName(action: String, app: Application, suffix: String? = null): String =
    listOf(
        action,
        app.name.takeIf { it != "main" }?.capitalize(Locale.ROOT),
        suffix?.capitalize(Locale.ROOT)
    ).filterNotNull().joinToString("")
