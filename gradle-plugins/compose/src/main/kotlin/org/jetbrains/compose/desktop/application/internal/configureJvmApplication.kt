/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.*
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.validation.validatePackageVersions
import org.jetbrains.compose.desktop.application.tasks.*
import org.jetbrains.compose.desktop.tasks.AbstractUnpackDefaultComposeApplicationResourcesTask
import org.jetbrains.compose.internal.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.util.*

private val defaultJvmArgs = listOf("-D$CONFIGURE_SWING_GLOBALS=true")

// todo: multiple launchers
// todo: file associations
// todo: use workers
internal fun configureJvmApplication(
    project: Project,
    app: JvmApplication,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    if (app._isDefaultConfigurationEnabled) {
        if (project.plugins.hasPlugin(KOTLIN_MPP_PLUGIN_ID)) {
            project.configureFromMppPlugin(app)
        } else if (project.plugins.hasPlugin(KOTLIN_JVM_PLUGIN_ID)) {
            val mainSourceSet = project.javaSourceSets.getByName("main")
            app.from(mainSourceSet)
        }
    }
    project.validatePackageVersions(app)
    project.configurePackagingTasks(listOf(app), unpackDefaultResources)
    project.configureWix()
}

internal fun Project.configureFromMppPlugin(mainApplication: JvmApplication) {
    var isJvmTargetConfigured = false
    mppExt.targets.all { target ->
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

internal fun Project.configurePackagingTasks(
    apps: Collection<JvmApplication>,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    for (app in apps) {
        val checkRuntime = tasks.composeDesktopJvmTask<AbstractCheckNativeDistributionRuntime>(
            taskName("checkRuntime", app)
        ) {
            javaHome.set(provider { app.javaHomeOrDefault() })
            javaRuntimePropertiesFile.set(
                project.layout.buildDirectory.file("compose/tmp/${app.name}/runtime-properties/properties.bin")
            )
        }

        tasks.composeDesktopJvmTask<AbstractSuggestModulesTask>(taskName("suggestRuntimeModules", app)) {
            dependsOn(checkRuntime)
            javaHome.set(provider { app.javaHomeOrDefault() })
            modules.set(provider { app.nativeDistributions.modules })

            app._configurationSource?.let { configSource ->
                dependsOn(configSource.jarTaskName)
                files.from(configSource.runtimeClasspath(project))
                launcherMainJar.set(app.mainJar.orElse(configSource.jarTask(project).flatMap { it.archiveFile }))
            }
        }

        val prepareAppResources = tasks.composeDesktopJvmTask<Sync>(
            taskName("prepareAppResources", app)
        ) {
            val appResourcesRootDir = app.nativeDistributions.appResourcesRootDir
            if (appResourcesRootDir.isPresent) {
                from(appResourcesRootDir.dir("common"))
                from(appResourcesRootDir.dir(currentOS.id))
                from(appResourcesRootDir.dir(currentTarget.id))
            }

            val destDir = project.layout.buildDirectory.dir("compose/tmp/${app.name}/resources")
            into(destDir)
        }

        val createRuntimeImage = tasks.composeDesktopJvmTask<AbstractJLinkTask>(
            taskName("createRuntimeImage", app)
        ) {
            dependsOn(checkRuntime)
            javaHome.set(provider { app.javaHomeOrDefault() })
            modules.set(provider { app.nativeDistributions.modules })
            includeAllModules.set(provider { app.nativeDistributions.includeAllModules })
            javaRuntimePropertiesFile.set(checkRuntime.flatMap { it.javaRuntimePropertiesFile })
            destinationDir.set(project.layout.buildDirectory.dir("compose/tmp/${app.name}/runtime"))
        }

        val createDistributable = tasks.composeDesktopJvmTask<AbstractJPackageTask>(
            taskName("createDistributable", app),
            args = listOf(TargetFormat.AppImage)
        ) {
            configurePackagingTask(
                app,
                createRuntimeImage = createRuntimeImage,
                prepareAppResources = prepareAppResources,
                checkRuntime = checkRuntime,
                unpackDefaultResources = unpackDefaultResources
            )
        }

        val packageFormats = app.nativeDistributions.targetFormats.map { targetFormat ->
            val packageFormat = tasks.composeDesktopJvmTask<AbstractJPackageTask>(
                taskName("package", app, targetFormat.name),
                args = listOf(targetFormat)
            ) {
                // On Mac we want to patch bundled Info.plist file,
                // so we create an app image, change its Info.plist,
                // then create an installer based on the app image.
                // We could create an installer the same way on other platforms, but
                // in some cases there are failures with JDK 15.
                // See [AbstractJPackageTask.patchInfoPlistIfNeeded]
                if (currentOS != OS.MacOS) {
                    configurePackagingTask(
                        app,
                        createRuntimeImage = createRuntimeImage,
                        prepareAppResources = prepareAppResources,
                        checkRuntime = checkRuntime,
                        unpackDefaultResources = unpackDefaultResources
                    )
                } else {
                    configurePackagingTask(
                        app,
                        createAppImage = createDistributable,
                        checkRuntime = checkRuntime,
                        unpackDefaultResources = unpackDefaultResources
                    )
                }
            }

            if (targetFormat.isCompatibleWith(OS.MacOS)) {
                check(targetFormat == TargetFormat.Dmg || targetFormat == TargetFormat.Pkg) {
                    "Unexpected target format for MacOS: $targetFormat"
                }

                val notarizationRequestsDir = project.layout.buildDirectory.dir("compose/notarization/${app.name}")
                val upload = tasks.composeDesktopJvmTask<AbstractUploadAppForNotarizationTask>(
                    taskName("notarize", app, targetFormat.name),
                    args = listOf(targetFormat)
                ) {
                    configureUploadForNotarizationTask(app, packageFormat, notarizationRequestsDir)
                }

                tasks.composeDesktopJvmTask<AbstractCheckNotarizationStatusTask>(
                    taskName("checkNotarizationStatus", app)
                ) {
                    configureCheckNotarizationStatusTask(app, notarizationRequestsDir)
                }
            }

            packageFormat
        }

        val packageAll = tasks.composeDesktopJvmTask<DefaultTask>(taskName("package", app)) {
            dependsOn(packageFormats)
        }

        val packageUberJarForCurrentOS = project.tasks.composeDesktopJvmTask<Jar>(taskName("package", app, "uberJarForCurrentOS")) {
            configurePackageUberJarForCurrentOS(app)
        }

        val runDistributable = project.tasks.composeDesktopJvmTask<AbstractRunDistributableTask>(
            taskName("runDistributable", app),
            args = listOf(createDistributable)
        )

        val run = project.tasks.composeDesktopJvmTask<JavaExec>(taskName("run", app)) {
            configureRunTask(app, prepareAppResources = prepareAppResources)
        }
    }
}

internal fun AbstractJPackageTask.configurePackagingTask(
    app: JvmApplication,
    createAppImage: TaskProvider<AbstractJPackageTask>? = null,
    createRuntimeImage: TaskProvider<AbstractJLinkTask>? = null,
    prepareAppResources: TaskProvider<Sync>? = null,
    checkRuntime: TaskProvider<AbstractCheckNativeDistributionRuntime>? = null,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    enabled = targetFormat.isCompatibleWithCurrentOS

    createAppImage?.let { createAppImage ->
        dependsOn(createAppImage)
        appImage.set(createAppImage.flatMap { it.destinationDir })
    }

    createRuntimeImage?.let { createRuntimeImage ->
        dependsOn(createRuntimeImage)
        runtimeImage.set(createRuntimeImage.flatMap { it.destinationDir })
    }

    prepareAppResources?.let { prepareResources ->
        dependsOn(prepareResources)
        val resourcesDir = project.layout.dir(prepareResources.map { it.destinationDir })
        appResourcesDir.set(resourcesDir)
    }

    checkRuntime?.let { checkRuntime ->
        dependsOn(checkRuntime)
        javaRuntimePropertiesFile.set(checkRuntime.flatMap { it.javaRuntimePropertiesFile })
    }

    configurePlatformSettings(app, unpackDefaultResources)

    app.nativeDistributions.let { executables ->
        packageName.set(app._packageNameProvider(project))
        packageDescription.set(provider { executables.description })
        packageCopyright.set(provider { executables.copyright })
        packageVendor.set(provider { executables.vendor })
        packageVersion.set(packageVersionFor(project, app, targetFormat))
        licenseFile.set(executables.licenseFile)
    }

    destinationDir.set(app.nativeDistributions.outputBaseDir.map { it.dir("${app.name}/${targetFormat.outputDirName}") })
    javaHome.set(provider { app.javaHomeOrDefault() })

    launcherMainJar.set(app.mainJar.orNull)
    files.from(app._fromFiles)
    dependsOn(*app._dependenciesTaskNames.toTypedArray())

    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        files.from(configSource.runtimeClasspath(project))
        launcherMainJar.set(app.mainJar.orElse(configSource.jarTask(project).flatMap { it.archiveFile }))
    }

    launcherMainClass.set(provider { app.mainClass })
    launcherJvmArgs.set(provider { defaultJvmArgs + app.jvmArgs })
    launcherArgs.set(provider { app.args })
}

internal fun AbstractUploadAppForNotarizationTask.configureUploadForNotarizationTask(
    app: JvmApplication,
    packageFormat: TaskProvider<AbstractJPackageTask>,
    requestsDir: Provider<Directory>
) {
    dependsOn(packageFormat)
    inputDir.set(packageFormat.flatMap { it.destinationDir })
    this.requestsDir.set(requestsDir)
    configureCommonNotarizationSettings(app)
}

internal fun AbstractCheckNotarizationStatusTask.configureCheckNotarizationStatusTask(
    app: JvmApplication,
    requestsDir: Provider<Directory>
) {
    requestDir.set(requestsDir)
    configureCommonNotarizationSettings(app)
}

internal fun AbstractNotarizationTask.configureCommonNotarizationSettings(
    app: JvmApplication
) {
    nonValidatedBundleID.set(app.nativeDistributions.macOS.bundleID)
    nonValidatedNotarizationSettings = app.nativeDistributions.macOS.notarization
}

internal fun AbstractJPackageTask.configurePlatformSettings(
    app: JvmApplication,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    dependsOn(unpackDefaultResources)
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
                iconFile.set(linux.iconFile.orElse(unpackDefaultResources.flatMap { it.resources.linuxIcon }))
                installationPath.set(linux.installationPath)
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
                iconFile.set(win.iconFile.orElse(unpackDefaultResources.flatMap { it.resources.windowsIcon }))
                installationPath.set(win.installationPath)
            }
        }
        OS.MacOS -> {
            app.nativeDistributions.macOS.also { mac ->
                macPackageName.set(provider { mac.packageName })
                macDockName.set(
                    if (mac.setDockNameSameAsPackageName)
                        provider { mac.dockName }.orElse(macPackageName).orElse(packageName)
                    else
                        provider { mac.dockName }
                )
                macAppStore.set(mac.appStore)
                macAppCategory.set(mac.appCategory)
                macEntitlementsFile.set(mac.entitlementsFile)
                macRuntimeEntitlementsFile.set(mac.runtimeEntitlementsFile)
                packageBuildVersion.set(packageBuildVersionFor(project, app, targetFormat))
                nonValidatedMacBundleID.set(provider { mac.bundleID })
                macProvisioningProfile.set(mac.provisioningProfile)
                macRuntimeProvisioningProfile.set(mac.runtimeProvisioningProfile)
                macExtraPlistKeysRawXml.set(provider { mac.infoPlistSettings.extraKeysRawXml })
                nonValidatedMacSigningSettings = app.nativeDistributions.macOS.signing
                iconFile.set(mac.iconFile.orElse(unpackDefaultResources.flatMap { it.resources.macIcon }))
                installationPath.set(mac.installationPath)
            }
        }
    }
}

private fun JavaExec.configureRunTask(
    app: JvmApplication,
    prepareAppResources: TaskProvider<Sync>
) {
    dependsOn(prepareAppResources)

    mainClass.set(provider { app.mainClass })
    executable(javaExecutable(app.javaHomeOrDefault()))
    jvmArgs = arrayListOf<String>().apply {
        addAll(defaultJvmArgs)

        if (currentOS == OS.MacOS) {
            val file = app.nativeDistributions.macOS.iconFile.ioFileOrNull
            if (file != null) add("-Xdock:icon=$file")
        }

        addAll(app.jvmArgs)
        val appResourcesDir = prepareAppResources.get().destinationDir
        add("-D$APP_RESOURCES_DIR=${appResourcesDir.absolutePath}")
    }
    args = app.args

    val cp = project.objects.fileCollection()
    // adding a null value will cause future invocations of `from` to throw an NPE
    app.mainJar.orNull?.let { cp.from(it) }
    cp.from(app._fromFiles)
    dependsOn(*app._dependenciesTaskNames.toTypedArray())

    app._configurationSource?.let { configSource ->
        dependsOn(configSource.jarTaskName)
        cp.from(configSource.runtimeClasspath(project))
    }

    classpath = cp
}

private fun Jar.configurePackageUberJarForCurrentOS(app: JvmApplication) {
        fun flattenJars(files: FileCollection): FileCollection =
            project.files({
                files.map { if (it.isZipOrJar()) project.zipTree(it) else it }
            })

        // adding a null value will cause future invocations of `from` to throw an NPE
        app.mainJar.orNull?.let { from(it) }
        from(flattenJars(app._fromFiles))
        dependsOn(*app._dependenciesTaskNames.toTypedArray())

        app._configurationSource?.let { configSource ->
            dependsOn(configSource.jarTaskName)
            from(flattenJars(configSource.runtimeClasspath(project)))
        }

        app.mainClass?.let { manifest.attributes["Main-Class"] = it }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveAppendix.set(currentTarget.id)
        archiveBaseName.set(app._packageNameProvider(project))
        archiveVersion.set(packageVersionFor(project, app, TargetFormat.AppImage))
        destinationDirectory.set(project.layout.buildDirectory.dir("compose/jars"))

        doLast {
            logger.lifecycle("The jar is written to ${archiveFile.ioFile.canonicalPath}")
        }
    }

private fun File.isZipOrJar() =
    name.endsWith(".jar", ignoreCase = true)
        || name.endsWith(".zip", ignoreCase = true)

internal fun JvmApplication.javaHomeOrDefault(): String =
    javaHome ?: System.getProperty("java.home")

private inline fun <reified T : Task> TaskContainer.composeDesktopJvmTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline configureFn: T.() -> Unit = {}
) = register(name, T::class.java, *args.toTypedArray()).apply {
    configure {
        it.group = "compose desktop"
        it.configureFn()
    }
}

internal fun JvmApplication._packageNameProvider(project: Project): Provider<String> =
    project.provider { nativeDistributions.packageName ?: project.name }

private fun taskName(action: String, app: JvmApplication, suffix: String? = null): String =
    listOfNotNull(
        action,
        app.name.takeIf { it != "main" }?.uppercaseFirstChar(),
        suffix?.uppercaseFirstChar()
    ).joinToString("")
