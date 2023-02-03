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

package androidx.build.testConfiguration

import com.android.build.api.variant.BuiltArtifact
import com.google.common.annotations.VisibleForTesting
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import java.io.File
import java.io.FileOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Helper class to record sha256 of APK files referenced in tests.
 *
 * It hashes the files the same way androidx-ci-action does to utilize the APK cache in Google Cloud
 * Storage (GCP). This hashing helps us avoid uploading the same APK multiple times to GCP.
 *
 * https://github.com/androidx/androidx-ci-action/blob/main/AndroidXCI/lib/src/main/kotlin/dev/androidx/ci/util/HashUtil.kt#L21
 */
@VisibleForTesting
class TestApkSha256Report {
    private val files = mutableMapOf<String, String>()

    /**
     * Adds the given builtArtifact to the list of shas after calculating its sha256 hash.
     */
    fun addFile(name: String, builtArtifact: BuiltArtifact) {
        addFile(name, File(builtArtifact.outputFile))
    }

    fun addFile(name: String, file: File) {
        require(file.exists()) {
            "Cannot find file ${file.path}"
        }
        val hash = sha256(file)
        val existing = files[name]
        require(existing == null || existing == hash) {
            "Same file name sent with different sha256 values. $name"
        }
        files[name] = hash
    }

    /**
     * Writes the [TestApkSha256Report] in XML format into the given [file].
     */
    fun writeToFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
        file.parentFile.mkdirs()
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.newDocument()
        val root = doc.createElement("sha256Report")
        doc.appendChild(root)
        files.entries.sortedBy {
            it.key
        }.forEach { (fileName, hash) ->
            val elm = doc.createElement("file")
            elm.setAttribute("name", fileName)
            elm.setAttribute("sha256", hash)
            root.appendChild(elm)
        }
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        val source = DOMSource(doc)
        FileOutputStream(file).use { fileOutput ->
            val result = StreamResult(fileOutput)
            transformer.transform(source, result)
        }
    }
}

@Suppress("UnstableApiUsage") // guava Hashing is marked as @Beta
internal fun sha256(file: File): String {
    val hasher = Hashing.sha256().newHasher()
    file.inputStream().buffered().use {
        while (it.available() > 0) {
            hasher.putBytes(it.readNBytes(1024))
        }
    }
    return BaseEncoding.base16().lowerCase().encode(
        hasher.hash().asBytes()
    )
}