/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.desktop.application.internal.files.copyZipEntry
import org.jetbrains.compose.desktop.application.internal.files.isJarFile
import org.jetbrains.compose.internal.utils.delete
import org.jetbrains.compose.internal.utils.ioFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


/**
 * This task flattens all jars from the input directory into the single one,
 * which is used later as a single source for uberjar.
 *
 * This task is necessary because the standard Jar/Zip task evaluates own `from()` args eagerly
 * [in the configuration phase](https://discuss.gradle.org/t/why-is-the-closure-in-from-method-of-copy-task-evaluated-in-config-phase/23469/4)
 * and snapshots an empty list of files in the Proguard destination directory,
 * instead of a list of real jars after Proguard task execution.
 *
 * Also, we use output to the single jar instead of flattening to the directory in the filesystem because:
 * - Windows filesystem is case-insensitive and not every jar can be unzipped without losing files
 * - it's just faster
 */
abstract class AbstractJarsFlattenTask : AbstractComposeDesktopTask() {

    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = objects.fileCollection()

    @get:OutputFile
    val flattenedJar: RegularFileProperty = objects.fileProperty()

    @get:Internal
    val seenEntryNames = hashSetOf<String>()

    @TaskAction
    fun execute() {
        seenEntryNames.clear()
        fileOperations.delete(flattenedJar)

        ZipOutputStream(FileOutputStream(flattenedJar.ioFile).buffered()).use { outputStream ->
            inputFiles.asFileTree.visit {
                when {
                    !it.isDirectory && it.file.isJarFile -> outputStream.writeJarContent(it.file)
                    !it.isDirectory -> outputStream.writeFile(it.file)
                }
            }
        }
    }

    private fun ZipOutputStream.writeJarContent(jarFile: File) =
        ZipInputStream(FileInputStream(jarFile)).use { inputStream ->
            var inputEntry: ZipEntry? = inputStream.nextEntry
            while (inputEntry != null) {
                writeEntryIfNotSeen(inputEntry, inputStream)
                inputEntry = inputStream.nextEntry
            }
        }

    private fun ZipOutputStream.writeFile(file: File) =
        FileInputStream(file).use { inputStream ->
            writeEntryIfNotSeen(ZipEntry(file.name), inputStream)
        }

    private fun ZipOutputStream.writeEntryIfNotSeen(entry: ZipEntry, inputStream: InputStream) {
        if (entry.name !in seenEntryNames) {
            copyZipEntry(entry, inputStream, this)
            seenEntryNames += entry.name
        }
    }
}