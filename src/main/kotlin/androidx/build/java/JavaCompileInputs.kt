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

package androidx.build.java

import androidx.build.doclava.androidJarFile
import androidx.build.multiplatformExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet

// JavaCompileInputs contains the information required to compile Java/Kotlin code
// This can be helpful for creating Metalava and Dokka tasks with the same settings
data class JavaCompileInputs(
    // Source files to process
    val sourcePaths: FileCollection,

    // Dependencies of [sourcePaths].
    val dependencyClasspath: FileCollection,

    // Android's boot classpath.
    val bootClasspath: FileCollection
) {
    companion object {
        // Constructs a JavaCompileInputs from a library and its variant
        fun fromLibraryVariant(
            variant: BaseVariant,
            project: Project
        ): JavaCompileInputs {
            val sourceCollection = getSourceCollection(variant, project)

            val dependencyClasspath = variant.getCompileClasspath(null).filter {
                it.exists()
            }

            return JavaCompileInputs(
                sourceCollection,
                dependencyClasspath,
                androidJarFile(project)
            )
        }

        // Constructs a JavaCompileInputs from a sourceset
        fun fromSourceSet(sourceSet: SourceSet, project: Project): JavaCompileInputs {
            val sourcePaths: FileCollection = project.files(
                project.provider({
                    sourceSet.allSource.srcDirs
                })
            )
            val dependencyClasspath = sourceSet.compileClasspath
            return JavaCompileInputs(sourcePaths, dependencyClasspath, androidJarFile(project))
        }

        private fun getSourceCollection(variant: BaseVariant, project: Project): FileCollection {
            // If the project has the kotlin-multiplatform plugin, we want to return a combined
            // collection of all the source files inside '*main' source sets. I.e, given a module
            // with a common and Android source set, this will look inside commonMain and
            // androidMain.
            val taskDependencies = mutableListOf<Any>(variant.javaCompileProvider)
            val sourceFiles = project.multiplatformExtension?.run {
                sourceSets
                    .filter { it.name.contains("main", ignoreCase = true) }
                    // TODO(igotti): come up with better filtering for non-Android sources.
                    .filterNot { it.name == "desktopMain" }
                    .flatMap { it.kotlin.sourceDirectories }
                    .also { require(it.isNotEmpty()) }
            } ?: project.provider({
                variant
                    .getSourceFolders(SourceKind.JAVA)
                    .map { folder ->
                        for (builtBy in folder.builtBy) {
                            taskDependencies.add(builtBy)
                        }
                        folder.dir
                    }
            })

            val sourceCollection = project.files(sourceFiles)
            for (dep in taskDependencies) {
                sourceCollection.builtBy(dep)
            }
            return sourceCollection
        }
    }
}
