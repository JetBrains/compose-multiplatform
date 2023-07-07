/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.inspection.gradle.InspectionPlugin
import androidx.inspection.gradle.createConsumeInspectionConfiguration
import androidx.inspection.gradle.createConsumeNonDexedInspectionConfiguration
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Sync
import java.io.File

/**
 * Copies artifacts prepared by InspectionPlugin into $destDir/inspection
 * and $destDir/inspection-nondexed
 */
fun Project.publishInspectionArtifacts() {
    publishInspectionConfiguration(
        "copyInspectionArtifacts",
        createConsumeInspectionConfiguration(),
        "inspection"
    )
    publishInspectionConfiguration(
        "copyUndexedInspectionArtifacts",
        createConsumeNonDexedInspectionConfiguration(),
        "inspection-nondexed"
    )
}

internal fun Project.publishInspectionConfiguration(
    name: String,
    configuration: Configuration,
    dirName: String
) {
    val topLevelProject = this
    subprojects { project ->
        project.afterEvaluate {
            if (project.plugins.hasPlugin(InspectionPlugin::class.java)) {
                topLevelProject.dependencies.add(configuration.name, project)
            }
        }
    }

    val sync = tasks.register(name, Sync::class.java) {
        it.dependsOn(configuration)
        it.from(configuration)
        it.destinationDir = File(getDistributionDirectory(), dirName)
    }
    addToBuildOnServer(sync)
}
