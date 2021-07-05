/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.*
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecResult
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import org.jetbrains.compose.desktop.application.dsl.InfoPlistSettings
import org.jetbrains.compose.desktop.application.dsl.MacOSSigningSettings
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.files.*
import org.jetbrains.compose.desktop.application.internal.files.MacJarSignFileCopyingProcessor
import org.jetbrains.compose.desktop.application.internal.files.fileHash
import org.jetbrains.compose.desktop.application.internal.files.transformJar
import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSSigningSettings
import org.jetbrains.compose.desktop.application.internal.validation.validate
import java.io.*
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.ArrayList

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
    val macPackageName: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    val macDockName: Property<String?> = objects.nullableProperty()

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

    @get:InputDirectory
    @get:Optional
    val runtimeImage: DirectoryProperty = objects.directoryProperty()

    @get:InputDirectory
    @get:Optional
    val appImage: DirectoryProperty = objects.directoryProperty()

    @get:Input
    @get:Optional
    internal val nonValidatedMacBundleID: Property<String?> = objects.nullableProperty()

    @get:Input
    @get:Optional
    internal val macExtraPlistKeysRawXml: Property<String?> = objects.nullableProperty()

    @get:Optional
    @get:Nested
    internal var nonValidatedMacSigningSettings: MacOSSigningSettings? = null

    private val macSigner: MacSigner? by lazy {
        val nonValidatedSettings = nonValidatedMacSigningSettings
        if (currentOS == OS.MacOS && nonValidatedSettings?.sign?.get() == true) {
            val validatedSettings = nonValidatedSettings.validate(nonValidatedMacBundleID)
            MacSigner(validatedSettings, runExternalTool)
        } else null
    }

    @get:LocalState
    protected val signDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/sign")

    @get:LocalState
    protected val resourcesDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/resources")

    @get:LocalState
    protected val skikoDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/skiko")

    @get:Internal
    private val libsDir: Provider<Directory> = workingDir.map {
        it.dir("libs")
    }

    @get:Internal
    private val libsMappingFile: Provider<RegularFile> = workingDir.map {
        it.file("libs-mapping.txt")
    }

    @get:Internal
    private val libsMapping = FilesMapping()

    override fun makeArgs(tmpDir: File): MutableList<String> = super.makeArgs(tmpDir).apply {
        if (targetFormat == TargetFormat.AppImage || appImage.orNull == null) {
            // Args, that can only be used, when creating an app image or an installer w/o --app-image parameter
            cliArg("--input", libsDir)
            cliArg("--runtime-image", runtimeImage)
            cliArg("--resource-dir", resourcesDir)

            val mappedJar = libsMapping[launcherMainJar.ioFile]?.singleOrNull()
                ?: error("Main jar was not processed correctly: ${launcherMainJar.ioFile}")
            cliArg("--main-jar", mappedJar)
            cliArg("--main-class", launcherMainClass)

            when (currentOS) {
                OS.Windows -> {
                    cliArg("--win-console", winConsole)
                }
            }
            cliArg("--icon", iconFile)
            launcherArgs.orNull?.forEach {
                cliArg("--arguments", "'$it'")
            }
            launcherJvmArgs.orNull?.forEach {
                cliArg("--java-options", "'$it'")
            }
            cliArg("--java-options", "'-Dskiko.library.path=${'$'}APPDIR'")
            if (currentOS == OS.MacOS) {
                macDockName.orNull?.let { dockName ->
                    cliArg("--java-options", "'-Xdock:name=$dockName'")
                }
            }
        }

        if (targetFormat != TargetFormat.AppImage) {
            // Args, that can only be used, when creating an installer
            cliArg("--app-image", appImage)
            cliArg("--install-dir", installationPath)
            cliArg("--license-file", licenseFile)

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
                OS.Windows -> {
                    cliArg("--win-dir-chooser", winDirChooser)
                    cliArg("--win-per-user-install", winPerUserInstall)
                    cliArg("--win-shortcut", winShortcut)
                    cliArg("--win-menu", winMenu)
                    cliArg("--win-menu-group", winMenuGroup)
                    cliArg("--win-upgrade-uuid", winUpgradeUuid)
                }
            }
        }

        cliArg("--type", targetFormat.id)

        cliArg("--dest", destinationDir)
        cliArg("--verbose", verbose)

        cliArg("--name", packageName)
        cliArg("--description", packageDescription)
        cliArg("--copyright", packageCopyright)
        cliArg("--app-version", packageVersion)
        cliArg("--vendor", packageVendor)

        when (currentOS) {
            OS.MacOS -> {
                cliArg("--mac-package-name", macPackageName)
                cliArg("--mac-package-identifier", nonValidatedMacBundleID)

                macSigner?.let { signer ->
                    cliArg("--mac-sign", true)
                    cliArg("--mac-signing-key-user-name", signer.settings.identity)
                    cliArg("--mac-signing-keychain", signer.settings.keychain)
                    cliArg("--mac-package-signing-prefix", signer.settings.prefix)
                }
            }
        }
    }

    private fun invalidateMappedLibs(
        inputChanges: InputChanges
    ): Set<File> {
        val outdatedLibs = HashSet<File>()
        val libsDirFile = libsDir.ioFile

        fun invalidateAllLibs() {
            outdatedLibs.addAll(files.files)
            outdatedLibs.add(launcherMainJar.ioFile)

            logger.debug("Clearing all files in working dir: $libsDirFile")
            fileOperations.delete(libsDirFile)
            libsDirFile.mkdirs()
        }

        if (inputChanges.isIncremental) {
            val allChanges = inputChanges.getFileChanges(files).asSequence() +
                    inputChanges.getFileChanges(launcherMainJar)

            try {
                for (change in allChanges) {
                    libsMapping.remove(change.file)?.let { files ->
                        files.forEach { fileOperations.delete(it) }
                    }
                    if (change.changeType != ChangeType.REMOVED) {
                        outdatedLibs.add(change.file)
                    }
                }
            } catch (e: Exception) {
                logger.debug("Could remove outdated libs incrementally: ${e.stacktraceToString()}")
                invalidateAllLibs()
            }
        } else {
            invalidateAllLibs()
        }

        return outdatedLibs
    }

    override fun prepareWorkingDir(inputChanges: InputChanges) {
        val libsDir = libsDir.ioFile
        val fileProcessor =
            macSigner?.let { signer ->
                val tmpDirForSign = signDir.ioFile
                fileOperations.delete(tmpDirForSign)
                tmpDirForSign.mkdirs()

                MacJarSignFileCopyingProcessor(signer, tmpDirForSign)
            } ?: SimpleFileCopyingProcessor
        fun copyFileToLibsDir(sourceFile: File): File {
            val targetFileName =
                if (sourceFile.isJarFile) "${sourceFile.nameWithoutExtension}-${fileHash(sourceFile)}.jar"
                else sourceFile.name
            val targetFile = libsDir.resolve(targetFileName)
            fileProcessor.copy(sourceFile, targetFile)
            return targetFile
        }

        val outdatedLibs = invalidateMappedLibs(inputChanges)
        for (sourceFile in outdatedLibs) {
            assert(sourceFile.exists()) { "Lib file does not exist: $sourceFile" }

            libsMapping[sourceFile] = if (isSkikoForCurrentOS(sourceFile)) {
                val unpackedFiles = unpackSkikoForCurrentOS(sourceFile, skikoDir.ioFile, fileOperations)
                unpackedFiles.map { copyFileToLibsDir(it) }
            } else {
                listOf(copyFileToLibsDir(sourceFile))
            }
        }

        fileOperations.delete(resourcesDir)
        fileOperations.mkdir(resourcesDir)
        if (currentOS == OS.MacOS) {
            InfoPlistBuilder(macExtraPlistKeysRawXml.orNull)
                .also { setInfoPlistValues(it) }
                .writeToFile(resourcesDir.ioFile.resolve("Info.plist"))
        }
    }

    override fun jvmToolEnvironment(): MutableMap<String, String> =
        super.jvmToolEnvironment().apply {
            if (currentOS == OS.Windows) {
                val wixDir = wixToolsetDir.ioFile
                val wixPath = wixDir.absolutePath
                val path = System.getenv("PATH") ?: ""
                put("PATH", "$wixPath;$path")
            }
        }


    override fun checkResult(result: ExecResult) {
        super.checkResult(result)
        val outputFile = findOutputFileOrDir(destinationDir.ioFile, targetFormat)
        logger.lifecycle("The distribution is written to ${outputFile.canonicalPath}")
    }

    override fun initState() {
        val mappingFile = libsMappingFile.ioFile
        if (mappingFile.exists()) {
            try {
                libsMapping.loadFrom(mappingFile)
            } catch (e: Exception) {
                fileOperations.delete(mappingFile)
                throw e
            }
            logger.debug("Loaded libs mapping from $mappingFile")
        }
    }

    override fun saveStateAfterFinish() {
        val mappingFile = libsMappingFile.ioFile
        libsMapping.saveTo(mappingFile)
        logger.debug("Saved libs mapping to $mappingFile")
    }

    private fun setInfoPlistValues(plist: InfoPlistBuilder) {
        check(currentOS == OS.MacOS) { "Current OS is not macOS: $currentOS" }

        plist[PlistKeys.LSMinimumSystemVersion] = "10.13"
        plist[PlistKeys.CFBundleDevelopmentRegion] = "English"
        plist[PlistKeys.CFBundleAllowMixedLocalizations] = "true"
        val packageName = packageName.get()
        plist[PlistKeys.CFBundleExecutable] = packageName
        plist[PlistKeys.CFBundleIconFile] = "$packageName.icns"
        val bundleId = nonValidatedMacBundleID.orNull
            ?: launcherMainClass.get().substringBeforeLast(".")
        plist[PlistKeys.CFBundleIdentifier] = bundleId
        plist[PlistKeys.CFBundleInfoDictionaryVersion] = "6.0"
        plist[PlistKeys.CFBundleName] = packageName
        plist[PlistKeys.CFBundlePackageType] = "APPL"
        val packageVersion = packageVersion.get()!!
        plist[PlistKeys.CFBundleShortVersionString] = packageVersion
        plist[PlistKeys.LSApplicationCategoryType] = "Unknown"
        plist[PlistKeys.CFBundleVersion] = packageVersion
        val year = Calendar.getInstance().get(Calendar.YEAR)
        plist[PlistKeys.NSHumanReadableCopyright] = packageCopyright.orNull
            ?: "Copyright (C) $year"
        plist[PlistKeys.NSSupportsAutomaticGraphicsSwitching] = "true"
        plist[PlistKeys.NSHighResolutionCapable] = "true"
    }
}

// Serializable is only needed to avoid breaking configuration cache:
// https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:requirements
private class FilesMapping : Serializable {
    private var mapping = HashMap<File, List<File>>()

    operator fun get(key: File): List<File>? =
        mapping[key]

    operator fun set(key: File, value: List<File>) {
        mapping[key] = value
    }

    fun remove(key: File): List<File>? =
        mapping.remove(key)

    fun loadFrom(mappingFile: File) {
        mappingFile.readLines().forEach { line ->
            if (line.isNotBlank()) {
                val paths = line.splitToSequence(File.pathSeparatorChar)
                val lib = File(paths.first())
                val mappedFiles = paths.drop(1).mapTo(ArrayList()) { File(it) }
                mapping[lib] = mappedFiles
            }
        }
    }

    fun saveTo(mappingFile: File) {
        mappingFile.parentFile.mkdirs()
        mappingFile.bufferedWriter().use { writer ->
            mapping.entries
                .sortedBy { (k, _) -> k.absolutePath }
                .forEach { (k, values) ->
                    (sequenceOf(k) + values.asSequence())
                        .joinTo(writer, separator = File.pathSeparator, transform = { it.absolutePath })
                }
        }
    }

    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeObject(mapping)
    }

    private fun readObject(stream: ObjectInputStream) {
        mapping = stream.readObject() as HashMap<File, List<File>>
    }
}

private fun isSkikoForCurrentOS(lib: File): Boolean =
    lib.name.startsWith("skiko-jvm-runtime-${currentOS.id}-${currentArch.id}")
            && lib.name.endsWith(".jar")

private fun unpackSkikoForCurrentOS(sourceJar: File, skikoDir: File, fileOperations: FileOperations): List<File> {
    val entriesToUnpack = when (currentOS) {
        OS.MacOS -> setOf("libskiko-macos-${currentArch.id}.dylib")
        OS.Windows -> setOf("skiko-windows-${currentArch.id}.dll", "icudtl.dat")
        OS.Linux -> setOf("libskiko-linux-${currentArch.id}.so")
    }

    // output files: unpacked libs, corresponding .sha256 files, and target jar
    val outputFiles = ArrayList<File>(entriesToUnpack.size * 2 + 1)
    val targetJar = skikoDir.resolve(sourceJar.name)
    outputFiles.add(targetJar)

    fileOperations.delete(skikoDir)
    fileOperations.mkdir(skikoDir)
    transformJar(sourceJar, targetJar) { zin, zout, entry ->
        // check both entry or entry.sha256
        if (entry.name.removeSuffix(".sha256") in entriesToUnpack) {
            val unpackedFile = skikoDir.resolve(entry.name.substringAfterLast("/"))
            zin.copyTo(unpackedFile)
            outputFiles.add(unpackedFile)
        } else {
            zout.withNewEntry(ZipEntry(entry)) {
                zin.copyTo(zout)
            }
        }
    }
    return outputFiles
}
