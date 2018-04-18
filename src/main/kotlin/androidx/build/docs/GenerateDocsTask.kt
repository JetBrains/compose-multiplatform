/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build.docs

import androidx.build.Version
import androidx.build.doclava.DoclavaTask
import java.io.File

open class GenerateDocsTask : DoclavaTask() {

    private data class Since(val path: String, val apiLevel: String)
    private data class Artifact(val path: String, val artifact: String)

    private val sinces = mutableListOf<Since>()
    private val artifacts = mutableListOf<Artifact>()

    fun addArtifactsAndSince() {
        doFirst {
            coreJavadocOptions {
                if (sinces.isNotEmpty()) {
                    addMultilineMultiValueOption("since").value = sinces.map { (path, apiLevel) ->
                        listOf(path, apiLevel)
                    }
                }

                if (artifacts.isNotEmpty()) {
                    addMultilineMultiValueOption("artifact").value = artifacts.map { artifact ->
                        listOf(artifact.path, artifact.artifact)
                    }
                }
            }
        }
    }

    fun addSinceFilesFrom(dir: File) {
        File(dir, "api").listFiles().forEach { file ->
            Version.parseOrNull(file)?.let { version ->
                sinces.add(Since(file.absolutePath, version.toString()))
            }
        }
    }

    fun addArtifact(path: String, artifact: String) = artifacts.add(Artifact(path, artifact))
}