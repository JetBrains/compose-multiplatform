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

package androidx.build

import androidx.build.gradle.isRoot
import org.gradle.api.Project
import java.io.File

fun isRunningOnBuildServer() = System.getenv("DIST_DIR") != null

/**
 * @return build id string for current build
 *
 * The build server does not pass the build id so we infer it from the last folder of the
 * distribution directory name.
 */
fun getBuildId(): String {
    return if (System.getenv("DIST_DIR") != null) File(System.getenv("DIST_DIR")).name else "0"
}

/**
 * The DIST_DIR is where you want to save things from the build. The build server will copy
 * the contents of DIST_DIR to somewhere and make it available.
 */
fun Project.getDistributionDirectory(): File {
    return if (System.getenv("DIST_DIR") != null) {
        File(System.getenv("DIST_DIR"))
    } else {
        val subdir = System.getProperty("DIST_SUBDIR") ?: ""
        File(getRootDirectory(this), "../../out/dist$subdir")
    }
}

/**
 * Directory to put build info files for release service dependency files.
 */
fun Project.getBuildInfoDirectory(): File =
        File(getDistributionDirectory(), "build-info")

/**
 * Directory to put host test results so they can be consumed by the testing dashboard.
 */
fun Project.getHostTestResultDirectory(): File =
        File(getDistributionDirectory(), "host-test-reports")

/**
 * Directory to put host test coverage results so they can be consumed by the testing dashboard.
 */
fun Project.getHostTestCoverageDirectory(): File =
    File(getDistributionDirectory(), "host-test-coverage")

private fun getRootDirectory(project: Project): File {
    val actualRootProject = if (project.isRoot) project else project.rootProject
    return actualRootProject.extensions.extraProperties.get("supportRootFolder") as File
}

/**
 * Whether the build should force all versions to be snapshots.
 */
fun isSnapshotBuild() = System.getenv("SNAPSHOT") != null

/**
 * Directory in a maven format to put all the publishing libraries.
 */
fun Project.getRepositoryDirectory(): File {
    val actualRootProject = if (project.isRoot) project else project.rootProject
    val directory = if (isSnapshotBuild()) {
        // For snapshot builds we put artifacts directly where downstream users can find them.
        File(actualRootProject.getDistributionDirectory(), "repository")
    } else {
        File(actualRootProject.buildDir, "support_repo")
    }
    directory.mkdirs()
    return directory
}
