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

package androidx.build.gmaven

import androidx.build.Version
import groovy.util.XmlSlurper
import groovy.util.slurpersupport.Node
import groovy.util.slurpersupport.NodeChild
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Queries maven.google.com to get the version numbers for each artifact.
 * Due to the structure of maven.google.com, a new query is necessary for each group.
 *
 * @param logger Logger of the root project. No reason to create multiple instances of this.
 */
class GMavenVersionChecker(private val logger: Logger) {
    private val versionCache: MutableMap<String, GroupVersionData> = HashMap()

    /**
     * Checks whether the given artifact is already on maven.google.com.
     *
     * @param group The project group on maven
     * @param artifactName The artifact name on maven
     * @param version The version on maven
     * @return true if the artifact is already on maven.google.com
     */
    fun isReleased(group: String, artifactName: String, version: String): Boolean {
        return getVersions(group, artifactName)?.contains(Version(version)) ?: false
    }

    /**
     * Return the available versions on maven.google.com for a given artifact
     *
     * @param group The group id of the artifact
     * @param artifactName The name of the artifact
     * @return The set of versions that are available on maven.google.com. Null if artifact is not
     *         available.
     */
    private fun getVersions(group: String, artifactName: String): Set<Version>? {
        val groupData = getVersionData(group)
        return groupData?.artifacts?.get(artifactName)?.versions
    }

    /**
     * Returns the version data for each artifact in a given group.
     * <p>
     * If data is not cached, this will make a web request to get it.
     *
     * @param group The group to query
     * @return A data class which has the versions for each artifact
     */
    private fun getVersionData(group: String): GroupVersionData? {
        return versionCache.getOrMaybePut(group) {
            fetchGroup(group, DEFAULT_RETRY_LIMIT)
        }
    }

    /**
     * Fetches the group version information from maven.google.com
     *
     * @param group The group name to fetch
     * @param retryCount Number of times we'll retry before failing
     * @return GroupVersionData that has the data or null if it is a new item.
     */
    private fun fetchGroup(group: String, retryCount: Int): GroupVersionData? {
        val url = buildGroupUrl(group)
        for (run in 0..retryCount) {
            logger.info("fetching maven XML from $url")
            try {
                val parsedXml = XmlSlurper(false, false).parse(url) as NodeChild
                return GroupVersionData.from(parsedXml)
            } catch (ignored: FileNotFoundException) {
                logger.info("could not find version data for $group, seems like a new file")
                return null
            } catch (ioException: IOException) {
                logger.warn("failed to fetch the maven info, retrying in 2 seconds. " +
                        "Run $run of $retryCount")
                Thread.sleep(RETRY_DELAY)
            }
        }
        throw GradleException("Could not access maven.google.com")
    }

    companion object {
        /**
         * Creates the URL which has the XML file that describes the available versions for each
         * artifact in that group
         *
         * @param group Maven group name
         * @return The URL of the XML file
         */
        private fun buildGroupUrl(group: String) =
                "$BASE${group.replace(".","/")}/$GROUP_FILE"
    }
}

private fun <K, V> MutableMap<K, V>.getOrMaybePut(key: K, defaultValue: () -> V?): V? {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        if (answer != null) put(key, answer)
        answer
    } else {
        value
    }
}

/**
 * Data class that holds the artifacts of a single maven group.
 *
 * @param name Maven group name
 * @param artifacts Map of artifact versions keyed by artifact name
 */
private data class GroupVersionData(
        val name: String,
        val artifacts: Map<String, ArtifactVersionData>
) {
    companion object {
        /**
         * Constructs an instance from the given node.
         *
         * @param xml The information node fetched from {@code GROUP_FILE}
         */
        fun from(xml: NodeChild): GroupVersionData {
            /*
             * sample input:
             * <android.arch.core>
             *   <runtime versions="1.0.0-alpha4,1.0.0-alpha5,1.0.0-alpha6,1.0.0-alpha7"/>
             *   <common versions="1.0.0-alpha4,1.0.0-alpha5,1.0.0-alpha6,1.0.0-alpha7"/>
             * </android.arch.core>
             */
            val name = xml.name()
            val artifacts: MutableMap<String, ArtifactVersionData> = HashMap()

            xml.childNodes().forEach {
                val node = it as Node
                val versions = (node.attributes()["versions"] as String).split(",").map {
                    if (it == "0.1" || it == "0.2" || it == "0.3") {
                        // androidx.core:core-ktx shipped versions 0.1, 0.2, and 0.3 which do not
                        // comply with our versioning scheme.
                        Version(it + ".0")
                    } else {
                        Version(it)
                    }
                }.toSet()
                artifacts.put(it.name(), ArtifactVersionData(it.name(), versions))
            }
            return GroupVersionData(name, artifacts)
        }
    }
}

/**
 * Data class that holds the version information about a single artifact
 *
 * @param name Name of the maven artifact
 * @param versions set of version codes that are already on maven.google.com
 */
private data class ArtifactVersionData(val name: String, val versions: Set<Version>)

// wait 2 seconds before retrying if fetch fails
private const val RETRY_DELAY: Long = 2000 // ms

// number of times we'll try to reach maven.google.com before failing
private const val DEFAULT_RETRY_LIMIT = 20

private const val BASE = "https://dl.google.com/dl/android/maven2/"
private const val GROUP_FILE = "group-index.xml"