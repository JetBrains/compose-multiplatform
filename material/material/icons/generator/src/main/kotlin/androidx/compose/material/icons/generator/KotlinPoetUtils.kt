/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.icons.generator

import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Writes the given [FileSpec] to [directory], appending a copyright notice to the beginning.
 * This is needed as this functionality isn't supported in KotlinPoet natively, and is not
 * intended to be supported. https://github.com/square/kotlinpoet/pull/514#issuecomment-441397363
 *
 * @param directory directory to write this [FileSpec] to
 * @param textTransform optional transformation to apply to the source file before writing to disk
 */
fun FileSpec.writeToWithCopyright(directory: File, textTransform: ((String) -> String)? = null) {
    var outputDirectory = directory

    if (packageName.isNotEmpty()) {
        for (packageComponent in packageName.split('.').dropLastWhile { it.isEmpty() }) {
            outputDirectory = outputDirectory.resolve(packageComponent)
        }
    }

    Files.createDirectories(outputDirectory.toPath())

    val file = outputDirectory.resolve("$name.kt")

    // Write this FileSpec to a StringBuilder, so we can process the text before writing to file.
    val fileContent = StringBuilder().run {
        writeTo(this)
        toString()
    }

    val transformedText = textTransform?.invoke(fileContent) ?: fileContent

    file.writeText(copyright + "\n\n" + transformedText)
}

/**
 * Sets the indent for this [FileSpec] to match that of our code style.
 */
fun FileSpec.Builder.setIndent() = indent(Indent)

// Code style indent is 4 spaces, compared to KotlinPoet's default of 2
private val Indent = " ".repeat(4)

/**
 * AOSP copyright notice. Given that we generate this code every build, it is never checked in,
 * so we should update the copyright with the current year every time we write to disk.
 */
private val copyright
    get() = """
    /*
     * Copyright $currentYear The Android Open Source Project
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
""".trimIndent()

private val currentYear: String get() = SimpleDateFormat("yyyy").format(Date())
