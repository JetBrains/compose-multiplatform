/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.build.checkapi

import androidx.build.Version
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.charset.Charset
import java.util.HashSet

/**
 * Task for updating the checked in API file with the newly generated one.
 */
open class UpdateApiTask : DefaultTask() {
    @get:InputFile
    lateinit var newApiFile: File
    @get:[InputFile Optional]
    var newRemovedApiFile: File? = null

    @get:[Input Optional]
    var whitelistErrors: Set<String> = HashSet()

    @get:OutputFile
    lateinit var oldApiFile: File
    @get:[OutputFile Optional]
    var oldRemovedApiFile: File? = null

    @get:[OutputFile Optional]
    var whitelistErrorsFile: File? = null

    /**
     * Actually copy the file to the desired location and update the whitelist warnings file.
     */
    @TaskAction
    fun doUpdate() {
        if (oldApiFile.exists() && newApiFile.readText() == oldApiFile.readText()) {
            // whatever nothing changed
            return
        }

        val version = Version(project.version as String)
        if (version.isPatch()) {
            throw GradleException("Public APIs may not be modified in patch releases.")
        } else if (version.isFinalApi() && oldApiFile.exists() && !project.hasProperty("force")) {
            throw GradleException("Public APIs may not be modified in finalized releases.")
        }

        Files.copy(newApiFile, oldApiFile)

        if (oldRemovedApiFile != null) {
            if (newRemovedApiFile != null) {
                Files.copy(newRemovedApiFile!!, oldRemovedApiFile!!)
            } else {
                oldRemovedApiFile!!.delete()
            }
        }

        if (whitelistErrorsFile != null && !whitelistErrors.isEmpty()) {
            Files.newWriter(
                    whitelistErrorsFile!!, Charset.defaultCharset()).use { writer ->
                for (error in whitelistErrors) {
                    writer.write(error + "\n")
                }
            }
            logger.lifecycle("Whitelisted " + whitelistErrors.size + " error(s)...")
        }

        logger.lifecycle("Wrote public API definition to " + oldApiFile.name)
    }
}
