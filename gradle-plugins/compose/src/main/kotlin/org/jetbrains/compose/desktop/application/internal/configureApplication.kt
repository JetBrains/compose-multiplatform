package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.*
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.Application
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.desktop.application.tasks.AbstractRunDistributableTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.util.*

// TODO: Multiple launchers
// TODO: File associations
// TODO: Use workers
fun configureApplicationImpl(project: Project, app: Application) {
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
        val run = project.tasks.composeTask<JavaExec>(taskName("run", app)) {
            configureRunTask(app)
        }

        val createRuntimeImage = tasks.composeTask<AbstractJLinkTask>(
            taskName("createRuntimeImage", app)
        ) {
            javaHome.set(provider { app.javaHomeOrDefault() })
            modules.set(provider { app.nativeDistributions.modules })
            destinationDir.set(project.layout.buildDirectory.dir("compose/tmp/${app.name}/runtime"))
        }

        val packageFormats = app.nativeDistributions.targetFormats.map { targetFormat ->
            tasks.composeTask<AbstractJPackageTask>(
                taskName("package", app, targetFormat.name),
                args = listOf(targetFormat)
            ) {
                configurePackagingTask(app, createRuntimeImage)
            }
        }

        val packageAll = tasks.composeTask<DefaultTask>(taskName("package", app)) {
            dependsOn(packageFormats)
        }

        val packageUberJarForCurrentOS = project.tasks.composeTask<Jar>(taskName("package", app, "uberJarForCurrentOS")) {
            configurePackageUberJarForCurrentOS(app)
        }

        val createDistributable = tasks.composeTask<AbstractJPackageTask>(
            taskName("createDistributable", app),
            args = listOf(TargetFormat.AppImage)
        ) {
            configurePackagingTask(app, createRuntimeImage)
        }

        val runDistributable = project.tasks.composeTask<AbstractRunDistributableTask>(
            taskName("runDistributable", app),
            args = listOf(createDistributable)
        )
    }
}

internal fun AbstractJPackageTask.configurePackagingTask(
    app: Application,
    createRuntimeImage: TaskProvider<AbstractJLinkTask>
) {
    enabled = targetFormat.isCompatibleWithCurrentOS

    val runtimeImageDir = createRuntimeImage.flatMap { it.destinationDir }
    dependsOn(createRuntimeImage)
    runtimeImage.set(runtimeImageDir)

    configurePlatformSettings(app)

    app.nativeDistributions.let { executables ->
        packageName.set(app._packageNameProvider(project))
        packageDescription.set(provider { executables.description })
        packageCopyright.set(provider { executables.copyright })
        packageVendor.set(provider { executables.vendor })
        packageVersion.set(app._packageVersionInternal(project))
    }

    destinationDir.set(app.nativeDistributions.outputBaseDir.map { it.dir("${app.name}/${targetFormat.outputDirName}") })
    javaHome.set(provider { app.javaHomeOrDefault() })

    launcherMainJar.set(app.mainJar.orNull)
    app._fromFiles.forEach { files.from(it) }
    dependsOn(*app._dependenciesTaskNames.toTypedArray())

    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        files.from(configSource.runtimeClasspath)
        launcherMainJar.set(app.mainJar.orElse(configSource.jarTask(project).flatMap { it.archiveFile }))
    }

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

private fun JavaExec.configureRunTask(app: Application) {
    mainClass.set(provider { app.mainClass })
    executable(javaExecutable(app.javaHomeOrDefault()))
    jvmArgs = app.jvmArgs
    args = app.args

    val cp = project.objects.fileCollection()
    // Adding a null value will cause future invocations of `from` to throw an NPE
    app.mainJar.orNull?.let { cp.from(it) }
    cp.from(app._fromFiles)
    dependsOn(*app._dependenciesTaskNames.toTypedArray())

    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        cp.from(configSource.runtimeClasspath)
    }

    classpath = cp
}

private fun Jar.configurePackageUberJarForCurrentOS(app: Application) {
        fun flattenJars(files: FileCollection): FileCollection =
            project.files({
                files.map { if (it.isZipOrJar()) project.zipTree(it) else it }
            })

        // Adding a null value will cause future invocations of `from` to throw an NPE
        app.mainJar.orNull?.let { from(it) }
        from(flattenJars(app._fromFiles))
        dependsOn(*app._dependenciesTaskNames.toTypedArray())

        app._configurationSource?.let { configSource ->
            dependsOn(configSource.jarTaskName)
            from(flattenJars(configSource.runtimeClasspath))
        }

        app.mainClass?.let { manifest.attributes["Main-Class"] = it }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveAppendix.set(currentTarget.id)
        archiveBaseName.set(app._packageNameProvider(project))
        archiveVersion.set(app._packageVersionInternal(project))
        destinationDirectory.set(project.layout.buildDirectory.dir("compose/jars"))

        doLast {
            logger.lifecycle("The jar is written to ${archiveFile.ioFile.canonicalPath}")
        }
    }

private fun File.isZipOrJar() =
    name.endsWith(".jar", ignoreCase = true)
        || name.endsWith(".zip", ignoreCase = true)

private fun Application.javaHomeOrDefault(): String =
    javaHome ?: System.getProperty("java.home")

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

internal fun Application._packageNameProvider(project: Project): Provider<String> =
    project.provider { nativeDistributions.packageName ?: project.name }

internal fun Application._packageVersionInternal(project: Project): Provider<String?> =
    project.provider {
        nativeDistributions.version
            ?: project.version.toString().takeIf { it != "unspecified" }
    }

@OptIn(ExperimentalStdlibApi::class)
private fun taskName(action: String, app: Application, suffix: String? = null): String =
    listOf(
        action,
        app.name.takeIf { it != "main" }?.capitalize(Locale.ROOT),
        suffix?.capitalize(Locale.ROOT)
    ).filterNotNull().joinToString("")
