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

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File

/**
 * A git client based on changeinfo files created by the build server.
 *
 * For sample config files, see:
 * ChangeInfoGitClientTest.kt
 * https://android-build.googleplex.com/builds/pending/P28356101/androidx_incremental/latest/incremental/P28356101-changeInfo
 *
 * For more information, see b/171569941
 */
private const val mainProject: String = "platform/frameworks/support"

class ChangeInfoGitClient(
    /**
     * The file containing the change information
     */
    private val config: String
) : GitClient {

    private val changeInfoParsed: JSONObject

    init {
       val parser = JSONParser()
       changeInfoParsed = parser.parse(config) as JSONObject
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
     * finds the most recently submitted change before any pending changes being tested
     */
    override fun findPreviousSubmittedChange(): String? {
        for (change in changesInThisRepo) {
            val revisions: JSONArray = change.get("revisions") as JSONArray
            for (revision in revisions) {
                val commit = (revision as JSONObject).get("commit") as JSONObject
                val parents = commit.get("parents") as JSONArray
                val firstParent = parents.get(0) as JSONObject
                return firstParent.get("commitId").toString()
            }
        }
        return null
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
        var latestCommit: String? = null
        for (change in changesInThisRepo) {
            val revisions: JSONArray = change.get("revisions") as JSONArray
            for (revision in revisions) {
                latestCommit = (revision as JSONObject).get("gitRevision").toString()
            }
        }
        if (latestCommit != null) {
            return listOf(Commit(latestCommit, fullProjectDir.toString()))
        }
        return listOf()
    }
}
