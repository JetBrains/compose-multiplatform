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

package androidx.build.paparazzi

import java.util.zip.ZipInputStream
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters.None
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Unzips one of Paparazzi's platform-specific layoutlib JAR artifacts so that Paparazzi can read
 * its contents at run time. These contain a native dynamic library and supporting resources
 * including ICU and fonts.
 */
@CacheableTransform
abstract class UnzipPaparazziNativeTransform : TransformAction<None> {
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputArtifact
    abstract val primaryInput: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = primaryInput.get().asFile
        val outputDir = outputs.dir(inputFile.nameWithoutExtension).also { it.mkdirs() }

        ZipInputStream(inputFile.inputStream().buffered()).use { zipInputStream ->
            while (true) {
                val entry = zipInputStream.nextEntry ?: break
                val outputFile = outputDir.resolve(entry.name)

                if (entry.isDirectory) {
                    outputFile.mkdir()
                } else {
                    // This works because ZipInputStream resizes itself to the contents of the
                    // last-returned entry
                    outputFile.outputStream().buffered().use { zipInputStream.copyTo(it) }
                }
            }
        }
    }
}