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

package androidx.build.dokka.kmpDocs

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

/**
 * Executes dokka CLI with the given classpath and the input file (json).
 */
internal fun runDokka(
    workerExecutor: WorkerExecutor,
    classpath: FileCollection,
    inputJson: File
) {
    workerExecutor.processIsolation().submit(
        DokkaWorkAction::class.java
    ) { parameters ->
        parameters.inputFile.set(inputJson)
        parameters.classpath.setFrom(classpath)
    }
}

internal interface DokkaParams : WorkParameters {
    val inputFile: RegularFileProperty
    val classpath: ConfigurableFileCollection
}

internal abstract class DokkaWorkAction @Inject constructor(
    private val execOperations: ExecOperations
) : WorkAction<DokkaParams> {
    override fun execute() {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val result = execOperations.javaexec {
            it.standardOutput = outputStream
            it.errorOutput = errorStream
            it.classpath = parameters.classpath
            it.args(parameters.inputFile.get().asFile.canonicalPath)
            it.isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            throw GradleException("Failed to run Dokka.\n ${errorStream.toString(Charsets.UTF_8)}")
        }
    }
}