/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.validation.validatePackageVersions
import org.jetbrains.compose.desktop.application.tasks.*
import org.jetbrains.compose.desktop.tasks.AbstractJarsFlattenTask
import org.jetbrains.compose.desktop.tasks.AbstractUnpackDefaultComposeApplicationResourcesTask
import org.jetbrains.compose.internal.utils.*
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.internal.utils.dir
import org.jetbrains.compose.internal.utils.ioFile
import org.jetbrains.compose.internal.utils.ioFileOrNull
import org.jetbrains.compose.internal.utils.javaExecutable
import org.jetbrains.compose.internal.utils.provider

private val defaultJvmArgs = listOf("-D$CONFIGURE_SWING_GLOBALS=true")
internal const val composeDesktopTaskGroup = "compose desktop"

// todo: multiple launchers
// todo: file associations
// todo: use workers
internal fun JvmApplicationContext.configureJvmApplication() {
    if (app.isDefaultConfigurationEnabled) {
        configureDefaultApp()
    }

    validatePackageVersions()
    val commonTasks = configureCommonJvmDesktopTasks()
    configurePackagingTasks(commonTasks)
    copy(buildType = app.buildTypes.release).configurePackagingTasks(commonTasks)
    if (currentOS == OS.Windows) {
        configureWix()
    }
}

internal class CommonJvmDesktopTasks(
    val unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>,
    val checkRuntime: TaskProvider<AbstractCheckNativeDistributionRuntime>,
    val suggestRuntimeModules: TaskProvider<AbstractSuggestModulesTask>,
    val prepareAppResources: TaskProvider<Sync>,
    val createRuntimeImage: TaskProvider<AbstractJLinkTask>
)

private fun JvmApplicationContext.configureCommonJvmDesktopTasks(): CommonJvmDesktopTasks {
    val unpackDefaultResources = tasks.register<AbstractUnpackDefaultComposeApplicationResourcesTask>(
        taskNameAction = "unpack",
        taskNameObject = "DefaultComposeDesktopJvmApplicationResources"
    ) {}

    val checkRuntime = tasks.register<AbstractCheckNativeDistributionRuntime>(
        taskNameAction = "check",
        taskNameObject = "runtime"
    ) {
        jdkHome.set(app.javaHomeProvider)
        checkJdkVendor.set(ComposeProperties.checkJdkVendor(project.providers))
        jdkVersionProbeJar.from(
            project.detachedComposeGradleDependency(
                artifactId = "gradle-plugin-internal-jdk-version-probe"
            ).excludeTransitiveDependencies()
        )
    }

    val suggestRuntimeModules = tasks.register<AbstractSuggestModulesTask>(
        taskNameAction = "suggest",
        taskNameObject = "runtimeModules"
    ) {
        dependsOn(checkRuntime)
        javaHome.set(app.javaHomeProvider)
        modules.set(provider { app.nativeDistributions.modules })

        useAppRuntimeFiles { (jarFiles, mainJar) ->
            files.from(jarFiles)
            launcherMainJar.set(mainJar)
        }
    }

    val prepareAppResources = tasks.register<Sync>(
        taskNameAction = "prepare",
        taskNameObject = "appResources"
    ) {
        val appResourcesRootDir = app.nativeDistributions.appResourcesRootDir
        if (appResourcesRootDir.isPresent) {
            from(appResourcesRootDir.dir("common"))
            from(appResourcesRootDir.dir(currentOS.id))
            from(appResourcesRootDir.dir(currentTarget.id))
        }
        into(jvmTmpDirForTask())
    }

    val createRuntimeImage = tasks.register<AbstractJLinkTask>(
        taskNameAction = "create",
        taskNameObject = "runtimeImage"
    ) {
        dependsOn(checkRuntime)
        javaHome.set(app.javaHomeProvider)
        modules.set(provider { app.nativeDistributions.modules })
        includeAllModules.set(provider { app.nativeDistributions.includeAllModules })
        javaRuntimePropertiesFile.set(checkRuntime.flatMap { it.javaRuntimePropertiesFile })
        destinationDir.set(appTmpDir.dir("runtime"))
    }

    return CommonJvmDesktopTasks(
        unpackDefaultResources,
        checkRuntime,
        suggestRuntimeModules,
        prepareAppResources,
        createRuntimeImage
    )
}

private fun JvmApplicationContext.configurePackagingTasks(
    commonTasks: CommonJvmDesktopTasks
) {
    val runProguard = if (buildType.proguard.isEnabled.orNull == true) {
        tasks.register<AbstractProguardTask>(
            taskNameAction = "proguard",
            taskNameObject = "Jars"
        ) {
            configureProguardTask(this, commonTasks.unpackDefaultResources)
        }
    } else null

    val createDistributable = tasks.register<AbstractJPackageTask>(
        taskNameAction = "create",
        taskNameObject = "distributable",
        args = listOf(TargetFormat.AppImage)
    ) {
        configurePackageTask(
            this,
            createRuntimeImage = commonTasks.createRuntimeImage,
            prepareAppResources = commonTasks.prepareAppResources,
            checkRuntime = commonTasks.checkRuntime,
            unpackDefaultResources = commonTasks.unpackDefaultResources,
            runProguard = runProguard
        )
    }

    val packageFormats = app.nativeDistributions.targetFormats.map { targetFormat ->
        val packageFormat = tasks.register<AbstractJPackageTask>(
            taskNameAction = "package",
            taskNameObject = targetFormat.name,
            args = listOf(targetFormat)
        ) {
            // On Mac we want to patch bundled Info.plist file,
            // so we create an app image, change its Info.plist,
            // then create an installer based on the app image.
            // We could create an installer the same way on other platforms, but
            // in some cases there are failures with JDK 15.
            // See [AbstractJPackageTask.patchInfoPlistIfNeeded]
            if (currentOS != OS.MacOS) {
                configurePackageTask(
                    this,
                    createRuntimeImage = commonTasks.createRuntimeImage,
                    prepareAppResources = commonTasks.prepareAppResources,
                    checkRuntime = commonTasks.checkRuntime,
                    unpackDefaultResources = commonTasks.unpackDefaultResources,
                    runProguard = runProguard
                )
            } else {
                configurePackageTask(
                    this,
                    createAppImage = createDistributable,
                    checkRuntime = commonTasks.checkRuntime,
                    unpackDefaultResources = commonTasks.unpackDefaultResources
                )
            }
        }

        if (targetFormat.isCompatibleWith(OS.MacOS)) {
            tasks.register<AbstractNotarizationTask>(
                taskNameAction = "notarize",
                taskNameObject = targetFormat.name,
                args = listOf(targetFormat)
            ) {
                dependsOn(packageFormat)
                inputDir.set(packageFormat.flatMap { it.destinationDir })
                configureCommonNotarizationSettings(this)
            }
        }

        packageFormat
    }

    val packageForCurrentOS = tasks.register<DefaultTask>(
        taskNameAction = "package",
        taskNameObject = "distributionForCurrentOS"
    ) {
        dependsOn(packageFormats)
    }

    if (buildType === app.buildTypes.default) {
        // todo: remove
        tasks.register<DefaultTask>("package") {
            dependsOn(packageForCurrentOS)

            doLast {
                it.logger.error(
                    "'${it.name}' task is deprecated and will be removed in next releases. " +
                    "Use '${packageForCurrentOS.get().name}' task instead")
            }
        }
    }

    val flattenJars = tasks.register<AbstractJarsFlattenTask>(
        taskNameAction = "flatten",
        taskNameObject = "Jars"
    ) {
        configureFlattenJars(this, runProguard)
    }

    val packageUberJarForCurrentOS = tasks.register<Jar>(
        taskNameAction = "package",
        taskNameObject = "uberJarForCurrentOS"
    ) {
        configurePackageUberJarForCurrentOS(this, flattenJars)
    }

    val runDistributable = tasks.register<AbstractRunDistributableTask>(
        taskNameAction = "run",
        taskNameObject = "distributable",
        args = listOf(createDistributable)
    )

    val run = tasks.register<JavaExec>(taskNameAction = "run") {
        configureRunTask(this, commonTasks.prepareAppResources, runProguard)
    }
}

private fun JvmApplicationContext.configureProguardTask(
    proguard: AbstractProguardTask,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
): AbstractProguardTask = proguard.apply {
    val settings = buildType.proguard
    mainClass.set(app.mainClass)
    proguardVersion.set(settings.version)
    proguardFiles.from(proguardVersion.map { proguardVersion ->
        project.detachedDependency(groupId = "com.guardsquare", artifactId = "proguard-gradle", version = proguardVersion)
    })
    configurationFiles.from(settings.configurationFiles)
    // ProGuard uses -dontobfuscate option to turn off obfuscation, which is enabled by default
    // We want to disable obfuscation by default, because often
    // it is not needed, but makes troubleshooting much harder.
    // If obfuscation is turned off by default,
    // enabling (`isObfuscationEnabled.set(true)`) seems much better,
    // than disabling obfuscation disabling (`dontObfuscate.set(false)`).
    // That's why a task property is follows ProGuard design,
    // when our DSL does the opposite.
    dontobfuscate.set(settings.obfuscate.map { !it })
    dontoptimize.set(settings.optimize.map { !it })

    joinOutputJars.set(settings.joinOutputJars)

    dependsOn(unpackDefaultResources)
    defaultComposeRulesFile.set(unpackDefaultResources.flatMap { it.resources.defaultComposeProguardRules })

    maxHeapSize.set(settings.maxHeapSize)
    destinationDir.set(appTmpDir.dir("proguard"))
    javaHome.set(app.javaHomeProvider)

    useAppRuntimeFiles { files ->
        inputFiles.from(files.allRuntimeJars)
        mainJar.set(files.mainJar)
    }
}

private fun JvmApplicationContext.configurePackageTask(
    packageTask: AbstractJPackageTask,
    createAppImage: TaskProvider<AbstractJPackageTask>? = null,
    createRuntimeImage: TaskProvider<AbstractJLinkTask>? = null,
    prepareAppResources: TaskProvider<Sync>? = null,
    checkRuntime: TaskProvider<AbstractCheckNativeDistributionRuntime>? = null,
    unpackDefaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>,
    runProguard: Provider<AbstractProguardTask>? = null
) {
    packageTask.enabled = packageTask.targetFormat.isCompatibleWithCurrentOS

    createAppImage?.let { createAppImage ->
        packageTask.dependsOn(createAppImage)
        packageTask.appImage.set(createAppImage.flatMap { it.destinationDir })
    }

    createRuntimeImage?.let { createRuntimeImage ->
        packageTask.dependsOn(createRuntimeImage)
        packageTask.runtimeImage.set(createRuntimeImage.flatMap { it.destinationDir })
    }

    prepareAppResources?.let { prepareResources ->
        packageTask.dependsOn(prepareResources)
        val resourcesDir = packageTask.project.layout.dir(prepareResources.map { it.destinationDir })
        packageTask.appResourcesDir.set(resourcesDir)
    }

    checkRuntime?.let { checkRuntime ->
        packageTask.dependsOn(checkRuntime)
        packageTask.javaRuntimePropertiesFile.set(checkRuntime.flatMap { it.javaRuntimePropertiesFile })
    }

    this.configurePlatformSettings(packageTask, unpackDefaultResources)

    app.nativeDistributions.let { executables ->
        packageTask.packageName.set(packageNameProvider)
        packageTask.packageDescription.set(packageTask.provider { executables.description })
        packageTask.packageCopyright.set(packageTask.provider { executables.copyright })
        packageTask.packageVendor.set(packageTask.provider { executables.vendor })
        packageTask.packageVersion.set(packageVersionFor(packageTask.targetFormat))
        packageTask.licenseFile.set(executables.licenseFile)
    }

    packageTask.destinationDir.set(app.nativeDistributions.outputBaseDir.map {
        it.dir("$appDirName/${packageTask.targetFormat.outputDirName}")
    })
    packageTask.javaHome.set(app.javaHomeProvider)

    if (runProguard != null) {
        packageTask.dependsOn(runProguard)
        packageTask.files.from(project.fileTree(runProguard.flatMap { it.destinationDir }))
        packageTask.launcherMainJar.set(runProguard.flatMap { it.mainJarInDestinationDir })
        packageTask.mangleJarFilesNames.set(false)
        packageTask.packageFromUberJar.set(runProguard.flatMap { it.joinOutputJars })
    } else {
        packageTask.useAppRuntimeFiles { (runtimeJars, mainJar) ->
            files.from(runtimeJars)
            launcherMainJar.set(mainJar)
        }
    }

    packageTask.launcherMainClass.set(provider { app.mainClass })
    packageTask.launcherJvmArgs.set(provider { defaultJvmArgs + app.jvmArgs })
    packageTask.launcherArgs.set(provider { app.args })
}

internal fun JvmApplicationContext.configureCommonNotarizationSettings(
    notarizationTask: AbstractNotarizationTask
) {
    notarizationTask.nonValidatedNotarizationSettings = app.nativeDistributions.macOS.notarization
}

private fun <T> TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>.get(
    fn: AbstractUnpackDefaultComposeApplicationResourcesTask.DefaultResourcesProvider.() -> Provider<T>
) = flatMap { fn(it.resources) }

internal fun JvmApplicationContext.configurePlatformSettings(
    packageTask: AbstractJPackageTask,
    defaultResources: TaskProvider<AbstractUnpackDefaultComposeApplicationResourcesTask>
) {
    packageTask.dependsOn(defaultResources)

    when (currentOS) {
        OS.Linux -> {
            app.nativeDistributions.linux.also { linux ->
                packageTask.linuxShortcut.set(provider { linux.shortcut })
                packageTask.linuxAppCategory.set(provider { linux.appCategory })
                packageTask.linuxAppRelease.set(provider { linux.appRelease })
                packageTask.linuxDebMaintainer.set(provider { linux.debMaintainer })
                packageTask.linuxMenuGroup.set(provider { linux.menuGroup })
                packageTask.linuxPackageName.set(provider { linux.packageName })
                packageTask.linuxRpmLicenseType.set(provider { linux.rpmLicenseType })
                packageTask.iconFile.set(linux.iconFile.orElse(defaultResources.get { linuxIcon }))
                packageTask.installationPath.set(linux.installationPath)
                packageTask.fileAssociations.set(provider { linux.fileAssociations })
            }
        }
        OS.Windows -> {
            app.nativeDistributions.windows.also { win ->
                packageTask.winConsole.set(provider { win.console })
                packageTask.winDirChooser.set(provider { win.dirChooser })
                packageTask.winPerUserInstall.set(provider { win.perUserInstall })
                packageTask.winShortcut.set(provider { win.shortcut })
                packageTask.winMenu.set(provider { win.menu })
                packageTask.winMenuGroup.set(provider { win.menuGroup })
                packageTask.winUpgradeUuid.set(provider { win.upgradeUuid })
                packageTask.iconFile.set(win.iconFile.orElse(defaultResources.get { windowsIcon }))
                packageTask.installationPath.set(win.installationPath)
                packageTask.fileAssociations.set(provider { win.fileAssociations })
            }
        }
        OS.MacOS -> {
            app.nativeDistributions.macOS.also { mac ->
                packageTask.macPackageName.set(provider { mac.packageName })
                packageTask.macDockName.set(
                    if (mac.setDockNameSameAsPackageName)
                        provider { mac.dockName }
                            .orElse(packageTask.macPackageName).orElse(packageTask.packageName)
                    else
                        provider { mac.dockName }
                )
                packageTask.macAppStore.set(mac.appStore)
                packageTask.macAppCategory.set(mac.appCategory)
                packageTask.macMinimumSystemVersion.set(mac.minimumSystemVersion)
                val defaultEntitlements = defaultResources.get { defaultEntitlements }
                packageTask.macEntitlementsFile.set(mac.entitlementsFile.orElse(defaultEntitlements))
                packageTask.macRuntimeEntitlementsFile.set(mac.runtimeEntitlementsFile.orElse(defaultEntitlements))
                packageTask.packageBuildVersion.set(packageBuildVersionFor(packageTask.targetFormat))
                packageTask.nonValidatedMacBundleID.set(provider { mac.bundleID })
                packageTask.macProvisioningProfile.set(mac.provisioningProfile)
                packageTask.macRuntimeProvisioningProfile.set(mac.runtimeProvisioningProfile)
                packageTask.macExtraPlistKeysRawXml.set(provider { mac.infoPlistSettings.extraKeysRawXml })
                packageTask.nonValidatedMacSigningSettings = app.nativeDistributions.macOS.signing
                packageTask.iconFile.set(mac.iconFile.orElse(defaultResources.get { macIcon }))
                packageTask.installationPath.set(mac.installationPath)
                packageTask.fileAssociations.set(provider { mac.fileAssociations })
            }
        }
    }
}

private fun JvmApplicationContext.configureRunTask(
    exec: JavaExec,
    prepareAppResources: TaskProvider<Sync>,
    runProguard: Provider<AbstractProguardTask>?
) {
    exec.dependsOn(prepareAppResources)

    exec.mainClass.set(exec.provider { app.mainClass })
    exec.executable(javaExecutable(app.javaHome))
    exec.jvmArgs = arrayListOf<String>().apply {
        addAll(defaultJvmArgs)

        if (currentOS == OS.MacOS) {
            val file = app.nativeDistributions.macOS.iconFile.ioFileOrNull
            if (file != null) add("-Xdock:icon=$file")
        }

        addAll(app.jvmArgs)
        val appResourcesDir = prepareAppResources.get().destinationDir
        add("-D$APP_RESOURCES_DIR=${appResourcesDir.absolutePath}")
    }
    exec.args = app.args

    if (runProguard != null) {
        exec.dependsOn(runProguard)
        exec.classpath = project.fileTree(runProguard.flatMap { it.destinationDir })
    } else {
        exec.useAppRuntimeFiles { (runtimeJars, _) ->
            classpath = runtimeJars
        }
    }
}

private fun JvmApplicationContext.configureFlattenJars(
    flattenJars: AbstractJarsFlattenTask,
    runProguard: Provider<AbstractProguardTask>?
) {
    if (runProguard != null) {
        flattenJars.dependsOn(runProguard)
        flattenJars.inputFiles.from(runProguard.flatMap { it.destinationDir })
    } else {
        flattenJars.useAppRuntimeFiles { (runtimeJars, _) ->
            inputFiles.from(runtimeJars)
        }
    }

    flattenJars.flattenedJar.set(appTmpDir.file("flattenJars/flattened.jar"))
}

private fun JvmApplicationContext.configurePackageUberJarForCurrentOS(
    jar: Jar,
    flattenJars: Provider<AbstractJarsFlattenTask>
) {
    jar.dependsOn(flattenJars)
    jar.from(project.zipTree(flattenJars.flatMap { it.flattenedJar }))

    app.mainClass?.let { jar.manifest.attributes["Main-Class"] = it }
    jar.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    jar.archiveAppendix.set(currentTarget.id)
    jar.archiveBaseName.set(packageNameProvider)
    jar.archiveVersion.set(packageVersionFor(TargetFormat.AppImage))
    jar.archiveClassifier.set(buildType.classifier)
    jar.destinationDirectory.set(jar.project.layout.buildDirectory.dir("compose/jars"))

    jar.doLast {
        jar.logger.lifecycle("The jar is written to ${jar.archiveFile.ioFile.canonicalPath}")
    }
}