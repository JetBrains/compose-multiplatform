/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.dependencyTracker

import org.gradle.api.logging.Logger
import java.io.File

/**
 * Utility class that can parse build.prop files and extract the sha's for frameworks/support.
 *
 * Currently, we don't use it since build system does not give us the right shas.
 */
object BuildPropParser {
    /**
     * Returns the sha which is the reference sha that we should use to find changed files.
     *
     * It returns null if an appropriate sha couldn't be found. (e.g. if more than 1 project changed
     * or  frameworks/support didn't change)
     *
     * @param appliedPropsFile The applied.props file that is usually located in the out folder.
     * It contains information about the build specific SHAs for this build for each
     * module
     *
     * @param repoPropsFile The repo.props file that is usually located in the out folder.
     * It contains the origin versions for each repository
     */
    fun getShaForThisBuild(
        appliedPropsFile: File,
        repoPropsFile: File,
        logger: Logger? = null
    ): BuildRange? {
        if (!appliedPropsFile.canRead()) {
            logger?.error(
                "cannot read applied props file from ${appliedPropsFile.absolutePath}"
            )
            return null
        }
        if (!repoPropsFile.canRead()) {
            logger?.error("cannot read repo props file from  ${repoPropsFile.absolutePath}")
            return null
        }
        val appliedProps = appliedPropsFile.readLines(Charsets.UTF_8).filterNot { it.isEmpty() }
        if (appliedProps.isEmpty() && appliedProps.size > 2) {
            logger?.info(
                """
                    We'll run everything because seems like too many things changed or nothing is
                    changed. Changed projects: $appliedProps
                """.trimIndent()
            )
            return null
        }
        val changedProject = appliedProps[0]
        if (changedProject.indexOf("frameworks/support") == -1) {
            logger?.info(
                """
                    Changed project is not frameworks/support. I'll run everything.
                    Changed project: $changedProject
                """.trimIndent()
            )
            return null
        }
        val changeSha = changedProject.split(" ").last()
        // now find it in repo props
        val androidXLineInRepo = repoPropsFile.readLines(Charsets.UTF_8).firstOrNull {
            it.indexOf("frameworks/support") >= 0
        }
        if (androidXLineInRepo == null) {
            logger?.info("Cannot find the androidX sha in repo props. $repoPropsFile")
            return null
        }
        val repoSha = androidXLineInRepo.split(" ").last()
        logger?.info("repo sha: $repoSha change sha: $changeSha")
        return BuildRange(
            buildSha = changeSha,
            repoSha = repoSha
        )
    }

    data class BuildRange(
        val repoSha: String,
        val buildSha: String
    )
}
