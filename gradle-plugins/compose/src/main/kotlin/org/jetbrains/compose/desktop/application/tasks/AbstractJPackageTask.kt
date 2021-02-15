package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecResult
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import org.jetbrains.compose.desktop.application.dsl.MacOSSigningSettings
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.desktop.application.internal.files.MacJarSignFileCopyingProcessor
import org.jetbrains.compose.desktop.application.internal.files.SimpleFileCopyingProcessor
import org.jetbrains.compose.desktop.application.internal.files.fileHash
import org.jetbrains.compose.desktop.application.internal.validation.ValidatedMacOSSigningSettings
import org.jetbrains.compose.desktop.application.internal.validation.validate
import java.io.File
import java.io.Serializable
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

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

    @get:Optional
    @get:Nested
    internal var nonValidatedMacSigningSettings: MacOSSigningSettings? = null

    private inline fun <T> withValidatedMacOSSigning(fn: (ValidatedMacOSSigningSettings) -> T): T? =
        nonValidatedMacSigningSettings?.let { nonValidated ->
            if (currentOS == OS.MacOS && nonValidated.sign.get()) {
                fn(nonValidated.validate(nonValidatedMacBundleID))
            } else null
        }

    @get:LocalState
    protected val signDir: Provider<Directory> = project.layout.buildDirectory.dir("compose/tmp/sign")

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
            cliArg("--input", tmpDir)
            cliArg("--runtime-image", runtimeImage)

            val mappedJar = libsMapping[launcherMainJar.ioFile]
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
                cliArg("--arguments", it)
            }
            launcherJvmArgs.orNull?.forEach {
                cliArg("--java-options", it)
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

                withValidatedMacOSSigning { signing ->
                    cliArg("--mac-sign", true)
                    cliArg("--mac-signing-key-user-name", signing.identity)
                    cliArg("--mac-signing-keychain", signing.keychain)
                    cliArg("--mac-package-signing-prefix", signing.prefix)
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
                    libsMapping.remove(change.file)?.let { fileOperations.delete(it) }
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
        val libsDirFile = libsDir.ioFile
        val fileProcessor =
            withValidatedMacOSSigning { signing ->
                val tmpDirForSign = signDir.ioFile
                fileOperations.delete(tmpDirForSign)
                tmpDirForSign.mkdirs()

                MacJarSignFileCopyingProcessor(
                    tempDir = tmpDirForSign,
                    execOperations = execOperations,
                    signing = signing
                )
            } ?: SimpleFileCopyingProcessor

        val outdatedLibs = invalidateMappedLibs(inputChanges)
        for (sourceFile in outdatedLibs) {
            assert(sourceFile.exists()) { "Lib file does not exist: $sourceFile" }

            val targetFileName =
                if (sourceFile.isJarFile)
                    "${sourceFile.nameWithoutExtension}-${fileHash(sourceFile)}.jar"
                else sourceFile.name
            val targetFile = libsDirFile.resolve(targetFileName)
            fileProcessor.copy(sourceFile, targetFile)
            libsMapping[sourceFile] = targetFile
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
}

// Serializable is only needed to avoid breaking configuration cache:
// https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:requirements
private class FilesMapping : Serializable {
    private var mapping = HashMap<File, File>()

    operator fun get(key: File): File? =
        mapping[key]

    operator fun set(key: File, value: File) {
        mapping[key] = value
    }

    fun remove(key: File): File? =
        mapping.remove(key)

    fun loadFrom(mappingFile: File) {
        mappingFile.readLines().forEach { line ->
            if (line.isNotBlank()) {
                val (k, v) = line.split(File.pathSeparatorChar)
                mapping[File(k)] = File(v)
            }
        }
    }

    fun saveTo(mappingFile: File) {
        mappingFile.parentFile.mkdirs()
        mappingFile.bufferedWriter().use { writer ->
            mapping.entries
                .sortedBy { (k, _) -> k.absolutePath }
                .forEach { (k, v) ->
                    writer.append(k.absolutePath)
                    writer.append(File.pathSeparatorChar)
                    writer.appendln(v.absolutePath)
                }
        }
    }

    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeObject(mapping)
    }

    private fun readObject(stream: ObjectInputStream) {
        mapping = stream.readObject() as HashMap<File, File>
    }
}