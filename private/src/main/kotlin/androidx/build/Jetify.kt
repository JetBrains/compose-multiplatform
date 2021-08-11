/*
 * Copyright 2019 The Android Open Source Project
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

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip

val archivesToDejetify = listOf(
    "m2repository/androidx/activity/**",
    "m2repository/androidx/ads/identifier/**",
    "m2repository/androidx/annotation/**",
    "m2repository/androidx/autofill/**",
    "m2repository/androidx/appcompat/**",
    "m2repository/androidx/arch/**",
    "m2repository/androidx/arch/core/**",
    "m2repository/androidx/asynclayoutinflater/**",
    "m2repository/androidx/benchmark/**",
    "m2repository/androidx/biometric/**",
    "m2repository/androidx/browser/**",
    "m2repository/androidx/camera/**",
    "m2repository/androidx/car/**",
    "m2repository/androidx/cardview/**",
    "m2repository/androidx/collection/collection/**",
    "m2repository/androidx/collection/collection-ktx/**",
    "m2repository/androidx/contentpager/**",
    "m2repository/androidx/coordinatorlayout/**",
    "m2repository/androidx/core/core/**",
    "m2repository/androidx/core/core-ktx/**",
    "m2repository/androidx/cursoradapter/**",
    "m2repository/androidx/customview/**",
    "m2repository/androidx/documentfile/**",
    "m2repository/androidx/drawerlayout/**",
    "m2repository/androidx/dynamicanimation/**",
    "m2repository/androidx/emoji/**",
    "m2repository/androidx/exifinterface/**",
    "m2repository/androidx/fragment/fragment/**",
    "m2repository/androidx/fragment/fragment-ktx/**",
    "m2repository/androidx/fragment/fragment-testing/**",
    "m2repository/androidx/gridlayout/**",
    "m2repository/androidx/heifwriter/**",
    "m2repository/androidx/interpolator/**",
    "m2repository/androidx/leanback/**",
    "m2repository/androidx/legacy/**",
    "m2repository/androidx/lifecycle/**",
    "m2repository/androidx/loader/**",
    "m2repository/androidx/localbroadcastmanager/**",
    "m2repository/androidx/media/media/**",
    "m2repository/androidx/mediarouter/**",
    "m2repository/androidx/navigation/**",
    "m2repository/androidx/palette/palette/**",
    "m2repository/androidx/percentlayout/**",
    "m2repository/androidx/preference/preference/**",
    "m2repository/androidx/print/**",
    "m2repository/androidx/paging/**",
    "m2repository/androidx/room/**",
    "m2repository/androidx/work/**",
    "m2repository/androidx/recommendation/**",
    "m2repository/androidx/recyclerview/**",
    "m2repository/androidx/remotecallback/**",
    "m2repository/androidx/savedstate/**",
    "m2repository/androidx/slice/slice-builders/**",
    "m2repository/androidx/slice/slice-core/**",
    "m2repository/androidx/slice/slice-view/**",
    "m2repository/androidx/slidingpanelayout/**",
    "m2repository/androidx/swiperefreshlayout/**",
    "m2repository/androidx/sqlite/**",
    "m2repository/androidx/textclassifier/**",
    "m2repository/androidx/transition/**",
    "m2repository/androidx/tvprovider/**",
    "m2repository/androidx/vectordrawable/**",
    "m2repository/androidx/versionedparcelable/**",
    "m2repository/androidx/viewpager/**",
    "m2repository/androidx/viewpager2/**",
    "m2repository/androidx/wear/**",
    "m2repository/androidx/webkit/**",
    "m2repository/androidx/media2/**",
    "m2repository/androidx/concurrent/**",
    "m2repository/androidx/sharetarget/**"
)

fun Project.partiallyDejetifyArchiveTask(archiveFile: Provider<RegularFile>): TaskProvider<Exec>? {
    return findProject(":jetifier:jetifier-standalone")?.let { standaloneProject ->
        val stripTask = stripArchiveForPartialDejetificationTask(archiveFile)

        tasks.register("partiallyDejetifyArchive", Exec::class.java) {
            val outputFileName = "${getDistributionDirectory().absolutePath}/" +
                "top-of-tree-m2repository-partially-dejetified-${getBuildId()}.zip"
            val jetifierBin = "${standaloneProject.buildDir}/install/jetifier-standalone/bin/" +
                "jetifier-standalone"
            val migrationConfig = "${standaloneProject.projectDir.getParentFile()}/migration.config"

            it.dependsOn(stripTask)
            it.inputs.file(stripTask.get().archiveFile)
            it.outputs.file(outputFileName)

            it.commandLine = listOf(
                jetifierBin,
                "-i", "${it.inputs.files.singleFile}",
                "-o", "${it.outputs.files.singleFile}",
                "-c", migrationConfig,
                "--log", "warning",
                "--reversed",
                "--rebuildTopOfTree"
            )
        }
    }
}

fun Project.stripArchiveForPartialDejetificationTask(archiveFile: Provider<RegularFile>):
    TaskProvider<Zip> {
        return tasks.register("stripArchiveForPartialDejetification", Zip::class.java) {
            it.dependsOn(rootProject.tasks.named(Release.FULL_ARCHIVE_TASK_NAME))
            it.from(zipTree(archiveFile))
            it.destinationDirectory.set(rootProject.buildDir)
            it.archiveFileName.set("stripped_archive_partial.zip")
            it.include(archivesToDejetify)
        }
    }
