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

package androidx.build.metalava

import androidx.build.SupportLibraryExtension
import androidx.build.androidJarFile
import androidx.build.hasApiFolder
import androidx.build.hasApiTasks
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

object Metalava {
    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }

        library.libraryVariants.all { variant ->
            if (variant.name == "release") {
                if (!project.hasApiFolder()) {
                    project.logger.info(
                        "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
                    return@all
                }

                val metalavaConfiguration = project.rootProject.configurations.getByName("metalava")
                val apiTxt = project.file("api/current.txt")

                val checkApi = project.tasks.create("checkApi", CheckApiTask::class.java) { task ->
                    task.configuration = metalavaConfiguration
                    task.bootClasspath = library.bootClasspath
                    task.setVariant(variant)
                    task.currentTxtFile = apiTxt

                    task.dependsOn(metalavaConfiguration)
                }
                project.tasks.getByName("check").dependsOn(checkApi)

                project.tasks.create("updateApi", UpdateApiTask::class.java) { task ->
                    task.configuration = metalavaConfiguration
                    task.bootClasspath = library.bootClasspath
                    task.setVariant(variant)
                    task.currentTxtFile = apiTxt

                    task.dependsOn(metalavaConfiguration)
                }
            }
        }
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        if (!hasApiTasks(project, extension)) {
            return
        }
        if (!project.hasApiFolder()) {
            project.logger.info(
                    "Project ${project.name} doesn't have an api folder, ignoring API tasks.")
            return
        }

        val metalavaConfiguration = project.rootProject.configurations.getByName("metalava")
        val apiTxt = project.file("api/current.txt")

        val sourceSets = project.properties["sourceSets"] as SourceSetContainer
        val mainSourceSet = sourceSets.getByName("main")

        val checkApi = project.tasks.create("checkApi", CheckApiTask::class.java) { task ->
            task.configuration = metalavaConfiguration
            task.bootClasspath = androidJarFile(project).files
            task.sourcePaths = mainSourceSet.allSource.srcDirs
            task.dependencyClasspath = mainSourceSet.compileClasspath
            task.currentTxtFile = apiTxt

            task.dependsOn(metalavaConfiguration)
        }
        project.tasks.getByName("check").dependsOn(checkApi)

        project.tasks.create("updateApi", UpdateApiTask::class.java) { task ->
            task.configuration = metalavaConfiguration
            task.bootClasspath = androidJarFile(project).files
            task.sourcePaths = mainSourceSet.allSource.srcDirs
            task.dependencyClasspath = mainSourceSet.compileClasspath
            task.currentTxtFile = apiTxt

            task.dependsOn(metalavaConfiguration)
        }
    }
}
