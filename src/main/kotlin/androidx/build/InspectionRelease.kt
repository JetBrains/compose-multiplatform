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

import androidx.inspection.gradle.DexInspectorTask
import androidx.inspection.gradle.InspectionPlugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import java.io.File

/**
 * Copies artifacts prepared by InspectionPlugin into $destDir/inspection
 */
fun Project.publishInspectionArtifacts() {
    val copy = tasks.register("copyInspectionArtifacts", Copy::class.java) {
        it.destinationDir = File(getDistributionDirectory(), "inspection")
    }
    addToBuildOnServer(copy)
    subprojects { project ->
        project.plugins.withType(InspectionPlugin::class.java) {
            project.tasks.withType(DexInspectorTask::class.java) { inspectionTask ->
                copy.configure {
                    it.from(inspectionTask.outputFile)
                    it.dependsOn(inspectionTask)
                }
            }
        }
    }
}
