/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.gmaven

import android.support.Version
import com.android.annotations.Nullable
import groovy.util.slurpersupport.NodeChild
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Queries maven.google.com to get the version numbers for each artifact.
 * Due to the structure of maven.google.com, a new query is necessary for each group.
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class GMavenVersionChecker {
    // wait 2 seconds before retrying if fetch fails
    private static final int RETRY_DELAY = 2000 // ms
    // number of times we'll try to reach maven.google.com before failing
    private static final int DEFAULT_RETRY_LIMIT = 20
    private static final String BASE = "https://dl.google.com/dl/android/maven2/"
    private static final String GROUP_FILE = "group-index.xml"

    // cache versions by group to avoid re-querying for each artifact
    private final Map<String, GroupVersionData> versionCache = new HashMap<>()
    // the logger from the project
    private final Logger logger

    /**
     * Creates a new instance using the given project's logger
     *
     * @param project This should be the root project. No reason to create multiple instances of
     *                this
     */
    GMavenVersionChecker(Project project) {
        this.logger = project.logger
    }

    /**
     * Creates the URL which has the XML file that describes the available versions for each
     * artifact in that group
     *
     * @param group Maven group name
     * @return The URL of the XML file
     */
    private static String buildGroupUrl(String group) {
        return BASE + group.replace(".", "/") + "/" + GROUP_FILE
    }

    /**
     * Returns the version data for each artifact in a given group.
     * <p>
     * If data is not cached, this will make a web request to get it.
     *
     * @param group The group to query
     * @return A data class which has the versions for each artifact
     */
    private GroupVersionData getVersionData(String group) {
        def versionData = versionCache.get(group)
        if (versionData == null) {
            versionData = fetchGroup(group)
            if (versionData != null) {
                versionCache.put(versionData.name, versionData)
            }
        }
        return versionData
    }

    /**
     * Fetches the group version information from maven.google.com
     *
     * @param group The group name to fetch
     * @param retryCount Number of times we'll retry before failing
     * @return GroupVersionData that has the data or null if it is a new item.
     */
    @Nullable
    private GroupVersionData fetchGroup(String group, int retryCount) {
        def url = buildGroupUrl(group)
        for (int run = 0; run < retryCount; run++) {
            logger.info "fetching maven XML from $url"
            try {
                def parsedXml = new XmlSlurper(false, false).parse(url)
                return new GroupVersionData(parsedXml)
            } catch (FileNotFoundException ignored) {
                logger.info "could not find version data for $group, seems like a new file"
                return null
            } catch (IOException ioException) {
                logger.warning "failed to fetch the maven info, retrying in 2 seconds. " +
                        "Run $run of $retryCount"
                Thread.sleep(RETRY_DELAY)
            }
        }
        throw new GradleException("Could not access maven.google.com")
    }

    /**
     * Fetches the group version information from maven.google.com
     *
     * @param group The group name to fetch
     * @return GroupVersionData that has the data or null if it is a new item.
     */
    @Nullable
    private GroupVersionData fetchGroup(String group) {
        return fetchGroup(group, DEFAULT_RETRY_LIMIT)
    }

    /**
     * Return the available versions on maven.google.com for a given artifact
     *
     * @param group The group id of the artifact
     * @param artifactName The name of the artifact
     * @return The set of versions that are available on maven.google.com. Null if artifact is not
     *         available.
     */
    @Nullable
    Set<Version> getVersions(String group, String artifactName) {
        def groupData = getVersionData(group)
        return groupData?.artifacts?.get(artifactName)?.versions
    }

    /**
     * Checks whether the given artifact is already on maven.google.com.
     *
     * @param group The project group on maven
     * @param artifactName The artifact name on maven
     * @param version The version on maven
     * @return true if the artifact is already on maven.google.com
     */
    boolean isReleased(String group, String artifactName, String version) {
        return getVersions(group, artifactName)?.contains(new Version(version))
    }

    /**
     * Data class that holds the artifacts of a single maven group
     */
    private static class GroupVersionData {
        /**
         * The group name
         */
        String name
        /**
         * Map of artifact versions keyed by artifact name
         */
        Map<String, ArtifactVersionData> artifacts = new HashMap<>()

        /**
         * Constructs an instance from the given node.
         *
         * @param xml The information node fetched from {@code GROUP_FILE}
         */
        GroupVersionData(NodeChild xml) {
            /**
             * sample input:
             * <android.arch.core>
             *   <runtime versions="1.0.0-alpha4,1.0.0-alpha5,1.0.0-alpha6,1.0.0-alpha7"/>
             *   <common versions="1.0.0-alpha4,1.0.0-alpha5,1.0.0-alpha6,1.0.0-alpha7"/>
             * </android.arch.core>
             */
            this.name = xml.name()
            xml.childNodes().each {
                def versions = it.attributes['versions'].split(",").collect { version ->
                    new Version(version)
                }.toSet()
                artifacts[it.name()] = new ArtifactVersionData(it.name(), versions)
            }
        }
    }

    /**
     * Data class that holds the version information about a single artifact
     */
    private static class ArtifactVersionData {
        /**
         * name of the artifact
         */
        final String name
        /**
         * set of version codes that are already on maven.google.com
         */
        final Set<Version> versions

        ArtifactVersionData(String name, Set<Version> versions) {
            this.name = name
            this.versions = versions
        }
    }
}