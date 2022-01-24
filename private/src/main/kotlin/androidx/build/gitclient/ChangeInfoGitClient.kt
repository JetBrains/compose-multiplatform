/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.gitclient

import org.gradle.api.GradleException
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File

/**
 * A git client based on changeinfo files and manifest files created by the build server.
 *
 * For sample changeinfo config files, see:
 * ChangeInfoGitClientTest.kt
 * https://android-build.googleplex.com/builds/pending/P28356101/androidx_incremental/latest/incremental/P28356101-changeInfo
 *
 * For more information, see b/171569941
 */
private const val mainProject: String = "platform/frameworks/support"

class ChangeInfoGitClient(
    /**
     * The file containing the information about which changes are new in this build
     */
    private val changeInfo: String,
    /**
     * The file containing version information
     */
    private val versionInfo: String
) : GitClient {

    private val changeInfoParsed: JSONObject

    init {
       val changeInfoParser = JSONParser()
       changeInfoParsed = changeInfoParser.parse(changeInfo) as JSONObject
    }

    private fun parseSupportVersion(config: String): String? {
        val revisionRegex = Regex("revision=\"([^\"]*)\"")
        for (line in config.split("\n")) {
            if (line.contains("path=\"frameworks/support\"")) {
                val result = revisionRegex.find(line)?.groupValues?.get(1)
                if (result != null) {
                    return result
                }
            }
        }
        throw GradleException("Could not identify frameworks/support version from text '$config'")
    }

    private val changesInThisRepo: List<JSONObject>
        get() {
          val allChanges: JSONArray? = changeInfoParsed.get("changes") as? JSONArray
          if (allChanges == null) {
              return listOf()
          } else {
              return allChanges.map({ change ->
                  change as JSONObject
              }).filter({ change ->
                  change.get("project") == mainProject
              })
          }
      }

    /**
     * Finds changed file paths
     */
    override fun findChangedFilesSince(
        sha: String, // unused in this implementation, the data file knows what is new
        top: String, // unused in this implementation, the data file knows what is new
        includeUncommitted: Boolean // unused in this implementation, not needed yet
    ): List<String> {
        if (includeUncommitted) {
            throw UnsupportedOperationException(
                "ChangeInfoGitClient does not support includeUncommitted == true yet"
            )
        }

        val fileList = mutableListOf<String>()
        val fileSet = mutableSetOf<String>()
        for (change in changesInThisRepo) {
            val revisions = change.get("revisions") as? JSONArray ?: listOf()
            for (revision in revisions) {
                val fileInfos = (revision as JSONObject).get("fileInfos") as? JSONArray ?: listOf()
                for (fileInfo in fileInfos) {
                    for (pathKey in listOf("oldPath", "path")) { // path and oldPath list files
                        val path = (fileInfo as JSONObject).get(pathKey)?.toString()
                        if (path != null) {
                            if (!fileSet.contains(path)) {
                                fileList.add(path)
                                fileSet.add(path)
                            }
                        }
                    }
                }
            }
        }
        return fileList
    }

    /**
     * Unused
     * If this were supported, it would:
     * Finds the most recently submitted change before any pending changes being tested
     */
    override fun findPreviousSubmittedChange(): String? {
        // findChangedFilesSince doesn't need this information, so
        // this is unsupported at the moment.
        // For now we just return a non-null string to signify that there was no error
        return ""
    }

    /**
     * Finds the commits in a certain range
     */
    override fun getGitLog(
        gitCommitRange: GitCommitRange,
        keepMerges: Boolean,
        fullProjectDir: File
    ): List<Commit> {
        if (gitCommitRange.n != 1) {
            throw UnsupportedOperationException(
                "ChangeInfoGitClient only supports n = 1, not ${gitCommitRange.n}"
            )
        }
        if (gitCommitRange.untilInclusive != "HEAD") {
            throw UnsupportedOperationException(
                "ChangeInfoGitClient only supports untilInclusive = HEAD, " +
                "not ${gitCommitRange.untilInclusive}"
            )
        }
        var latestCommit: String? = parseSupportVersion(versionInfo)
        if (latestCommit != null) {
            return listOf(Commit("_CommitSHA:$latestCommit", fullProjectDir.toString()))
        }
        return listOf()
    }
}
