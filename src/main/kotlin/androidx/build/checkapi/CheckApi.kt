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

package androidx.build.checkapi

import androidx.build.AndroidXExtension
import androidx.build.Version
import androidx.build.version
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

enum class ApiType {
    CLASSAPI,
    RESOURCEAPI
}

fun Project.hasApiFolder() = File(projectDir, "api").exists()

fun hasApiTasks(project: Project, extension: AndroidXExtension): Boolean {
    if (extension.toolingProject) {
        project.logger.info("Project ${project.name} is tooling project ignoring API tasks.")
        return false
    }

    if (project.hasApiFolder()) {
        return true
    }

    if (!extension.publish.shouldRelease()) {
        project.logger.info("Project ${project.name} is not published, ignoring API tasks." +
                "If you still want to trackApi, simply create \"api\" folder in your project path")
        return false
    }

    if (extension.publish.shouldRelease() && project.version().isFinalApi()) {
        throw GradleException("Project ${project.name} must track API before stabilizing API\n." +
                "To do that create \"api\" in your project directory and " +
                "run \"./gradlew updateApi\" command")
    }
    return false
}

/**
 * Returns the API file whose contents match the project's source code.
 * This is the API file that the updateApi task will write to.
 * Note that in many cases the filename will be current.txt but not always (such as for release versions).
 *
 * @param project the project to query
 * @return the current api file for that project
 */
fun Project.getCurrentApiFile() = getApiFile(project.projectDir, project.version())

/**
 * Same as getCurrentApiFile but also contains a restricted API file too
 */
fun Project.getCurrentApiLocation() = ApiLocation.fromPublicApiFile(project.getCurrentApiFile())

/**
 * Returns the API file containing the public API that this library promises to support
 * This is API file that checkApiRelease validates against
 * @return the API file
 */
fun Project.getRequiredCompatibilityApiFile() =
        getRequiredCompatibilityApiFileFromDir(File(project.projectDir, "api"), project.version(),
            ApiType.CLASSAPI)

/*
 * Same as getRequiredCompatibilityApiFile but also contains a restricted API file
 */
fun Project.getRequiredCompatibilityApiLocation(): ApiLocation? {
    val publicFile = project.getRequiredCompatibilityApiFile()
    if (publicFile == null) {
        return null
    }
    return ApiLocation.fromPublicApiFile(publicFile)
}

/**
 * Returns the API file for the API of the specified version.
 *
 * @param version the API version, ex. 25.0.0-SNAPSHOT
 * @return the API file of this version
 */
private fun getApiFile(rootDir: File, version: Version): File {
    if (version.patch != 0 && (version.isAlpha() || version.isBeta())) {
        val suggestedVersion = Version("${version.major}.${version.minor}.${version.patch}-rc01")
        throw GradleException("Illegal version $version . It is not allowed to have a nonzero " +
                "patch number and be alpha or beta at the same time.\n" +
                "Did you mean $suggestedVersion?")
    }

    var extra = ""
    if (version.patch == 0 && version.extra != null) {
        extra = version.extra
    }
    val apiDir = File(rootDir, "api")
    return File(apiDir, "${version.major}.${version.minor}.0$extra.txt")
}

/**
 * Returns the api file that version <version> is required to be compatible with.
 * If apiType is RESOURCEAPI, it will return the resource api file and if it is CLASSAPI, it will
 * return the regular api file.
 */
fun getRequiredCompatibilityApiFileFromDir(
    apiDir: File,
    version: Version,
    apiType: ApiType
): File? {
    var lastFile: File? = null
    var lastVersion: Version? = null
    var apiFiles = apiDir.listFiles().toList()
    apiFiles = apiFiles.filter { (apiType == ApiType.RESOURCEAPI && it.name.startsWith("res")) ||
            (apiType == ApiType.CLASSAPI && !it.name.startsWith("res")) }
    apiFiles.forEach { file ->
        val parsed = Version.parseOrNull(file)
        parsed?.let { otherVersion ->
            if ((lastFile == null || lastVersion!! < otherVersion) &&
                    (otherVersion < version) &&
                    (otherVersion.isFinalApi()) &&
                    (otherVersion.major == version.major)) {
                lastFile = file
                lastVersion = otherVersion
            }
        }
    }
    return lastFile
}
