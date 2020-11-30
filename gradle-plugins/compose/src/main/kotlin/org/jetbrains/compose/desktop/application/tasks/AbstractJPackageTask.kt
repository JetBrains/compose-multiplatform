package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.*
import java.io.File
import javax.inject.Inject

abstract class AbstractJPackageTask @Inject constructor(
    @get:Input
    val targetFormat: TargetFormat,
) : AbstractJvmToolOperationTask("jpackage") {
    @get:InputFiles
    val files: ConfigurableFileCollection = objects.fileCollection()

    @get:InputDirectory
    @get:Optional
    /** @see internal/wixToolset.kt */
    val wixToolsetDir: DirectoryProperty = objects.directoryProperty()

    @get:Input
    @get:Optional
    val installationPath: Property<String?> = objects.nullableProperty()

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val licenseFile: RegularFileProperty = objects.fileProperty()

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val iconFile: RegularFileProperty = objects.fileProperty()

    @get:Input
    val launcherMainClass: Property<String> = objects.notNullProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val launcherMainJar: RegularFileProperty = objects.fileProperty()

    @get:Input
    @get:Optional
    val launcherArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Input
    @get:Optional
    val launcherJvmArgs: ListProperty<String> = objects.listProperty(String::class.java)

    @get:Input
    val packageName: Property<String> = objects.notNullProperty()

    @get:Input
    @get:Optional
    val packageDescription: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val packageCopyright: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val packageVendor: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val packageVersion: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxShortcut: Property<Boolean?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxPackageName: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxAppRelease: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxAppCategory: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxDebMaintainer: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxMenuGroup: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val linuxRpmLicenseType: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val macPackageIdentifier: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val macPackageName: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val macBundleSigningPrefix: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val macSign: Property<Boolean?> = objects.nullableProperty()

    @get:InputFile
    @get:Optional
    val macSigningKeychain: RegularFileProperty = objects.fileProperty()

    @get:Input
    @get:Optional
    val macSigningKeyUserName: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winConsole: Property<Boolean?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winDirChooser: Property<Boolean?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winPerUserInstall: Property<Boolean?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winShortcut: Property<Boolean?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winMenu: Property<Boolean?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winMenuGroup: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val winUpgradeUuid: Property<String?> = objects.nullableProperty()

    @get:Input
    val modules: ListProperty<String> = objects.listProperty(String::class.java)

    override fun makeArgs(tmpDir: File): MutableList<String> = super.makeArgs(tmpDir).apply {
        cliArg("--input", tmpDir)
        cliArg("--type", targetFormat.id)

        cliArg("--dest", destinationDir.asFile.get())
        cliArg("--verbose", verbose)

        cliArg("--install-dir", installationPath)
        cliArg("--license-file", licenseFile.asFile.orNull)
        cliArg("--icon", iconFile.asFile.orNull)

        cliArg("--name", packageName)
        cliArg("--description", packageDescription)
        cliArg("--copyright", packageCopyright)
        cliArg("--app-version", packageVersion)
        cliArg("--vendor", packageVendor)

        cliArg("--main-jar", launcherMainJar.asFile.get().name)
        cliArg("--main-class", launcherMainClass)
        launcherArgs.orNull?.forEach {
            cliArg("--arguments", it)
        }
        launcherJvmArgs.orNull?.forEach {
            cliArg("--java-options", it)
        }

        when (currentOS) {
            OS.Linux -> {
                cliArg("--linux-shortcut", linuxShortcut)
                cliArg("--linux-package-name", linuxPackageName)
                cliArg("--linux-app-release", linuxAppRelease)
                cliArg("--linux-app-category", linuxAppCategory)
                cliArg("--linux-deb-maintainer", linuxDebMaintainer)
                cliArg("--linux-menu-group", linuxMenuGroup)
                cliArg("--linux-rpm-license-type", linuxRpmLicenseType)
            }
            OS.MacOS -> {
                cliArg("--mac-package-identifier", macPackageIdentifier)
                cliArg("--mac-package-name", macPackageName)
                cliArg("--mac-bundle-signing-prefix", macBundleSigningPrefix)
                cliArg("--mac-sign", macSign)
                cliArg("--mac-signing-keychain", macSigningKeychain.asFile.orNull)
                cliArg("--mac-signing-key-user-name", macSigningKeyUserName)
            }
            OS.Windows -> {
                cliArg("--win-console", winConsole)
                cliArg("--win-dir-chooser", winDirChooser)
                cliArg("--win-per-user-install", winPerUserInstall)
                cliArg("--win-shortcut", winShortcut)
                cliArg("--win-menu", winMenu)
                cliArg("--win-menu-group", winMenuGroup)
                cliArg("--win-upgrade-uuid", winUpgradeUuid)
            }
        }

        modules.get().forEach { m ->
            cliArg("--add-modules", m)
        }
    }

    override fun prepareWorkingDir(tmpDir: File) {
        super.prepareWorkingDir(tmpDir)

        launcherMainJar.asFile.orNull?.let { sourceFile ->
            val targetFile = tmpDir.resolve(sourceFile.name)
            sourceFile.copyTo(targetFile)
        }

        val myFiles = files
        fileOperations.copy {
            it.from(myFiles)
            it.into(tmpDir)
        }
    }

    override fun configureExec(exec: ExecSpec) {
        super.configureExec(exec)
        configureWixPathIfNeeded(exec)
    }

    private fun configureWixPathIfNeeded(exec: ExecSpec) {
        if (currentOS == OS.Windows) {
            val wixDir = wixToolsetDir.asFile.orNull ?: return
            val wixPath = wixDir.absolutePath
            val path = System.getenv("PATH") ?: ""
            exec.environment("PATH", "$wixPath;$path")
        }
    }

    override fun checkResult(result: ExecResult) {
        super.checkResult(result)

        val destinationDirFile = destinationDir.asFile.get()
        val finalLocation = when (targetFormat) {
            TargetFormat.AppImage -> destinationDirFile
            else -> destinationDirFile.walk().first { it.isFile && it.name.endsWith(targetFormat.fileExt) }
        }
        logger.lifecycle("The distribution is written to ${finalLocation.canonicalPath}")
    }
}