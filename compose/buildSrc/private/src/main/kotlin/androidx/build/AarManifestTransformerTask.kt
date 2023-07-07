/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipFile
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Transforms an AAR by removing the `android:targetSdkVersion` element from the manifest.
 */
@CacheableTask
abstract class AarManifestTransformerTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aarFile: RegularFileProperty

    @get:OutputFile
    abstract val updatedAarFile: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val aar = aarFile.get().asFile
        val updatedAar = updatedAarFile.get().asFile
        val tempDir = Files.createTempDirectory("${name}Unzip").toFile()
        tempDir.deleteOnExit()

        ZipFile(aar).use { aarFile ->
            aarFile.unzipTo(tempDir)
        }

        val manifestFile = File(tempDir, "AndroidManifest.xml")
        manifestFile.writeText(removeTargetSdkVersion(manifestFile.readText()))

        tempDir.zipTo(updatedAar)
        tempDir.deleteRecursively()
    }
}

/**
 * Removes the `android:targetSdkVersion` element from the [manifest].
 */
fun removeTargetSdkVersion(manifest: String): String = manifest.replace(
    "\\s*android:targetSdkVersion=\".+?\"".toRegex(),
    ""
)

private fun ZipFile.unzipTo(tempDir: File) {
    entries.iterator().forEach { entry ->
        if (entry.isDirectory) {
            File(tempDir, entry.name).mkdirs()
        } else {
            val file = File(tempDir, entry.name)
            file.parentFile.mkdirs()
            getInputStream(entry).use { stream ->
                file.writeBytes(stream.readBytes())
            }
        }
    }
}

private fun File.zipTo(outZip: File) {
    ZipOutputStream(outZip.outputStream()).use { stream ->
        listFiles()!!.forEach { file ->
            stream.addFileRecursive(null, file)
        }
    }
}

private fun ZipOutputStream.addFileRecursive(parentPath: String?, file: File) {
    val entryPath = if (parentPath != null) "$parentPath/${file.name}" else file.name
    val entry = ZipEntry(file, entryPath)

    // Reset creation time of entry to make it deterministic.
    entry.time = 0
    entry.creationTime = FileTime.fromMillis(0)

    if (file.isFile) {
        putNextEntry(entry)
        file.inputStream().use { stream ->
            stream.copyTo(this)
        }
        closeEntry()
    } else if (file.isDirectory) {
        val listFiles = file.listFiles()
        if (!listFiles.isNullOrEmpty()) {
            putNextEntry(entry)
            closeEntry()
            listFiles.forEach { containedFile ->
                addFileRecursive(entryPath, containedFile)
            }
        }
    }
}
