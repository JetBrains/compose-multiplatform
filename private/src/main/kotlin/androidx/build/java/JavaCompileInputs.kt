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

import androidx.build.getAndroidJar
import androidx.build.multiplatformExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

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
        @Suppress("DEPRECATION") // BaseVariant
        fun fromLibraryVariant(
            variant: com.android.build.gradle.api.BaseVariant,
            project: Project,
            bootClasspath: FileCollection
        ): JavaCompileInputs {
            val sourceCollection = getSourceCollection(variant, project)

            val dependencyClasspath = variant.getCompileClasspath(null).filter {
                it.exists()
            }

            return JavaCompileInputs(
                sourceCollection,
                dependencyClasspath,
                bootClasspath
            )
        }

        /**
         * Returns the JavaCompileInputs for the `jvm` target of a KMP project.
         *
         * @param project The project whose main jvm target inputs will be returned.
         */
        fun fromKmpJvmTarget(
            project: Project
        ): JavaCompileInputs {
            val kmpExtension = checkNotNull(project.multiplatformExtension) {
                """
                ${project.path} needs to have Kotlin Multiplatform Plugin applied to obtain its
                jvm source sets.
                """.trimIndent()
            }
            val jvmTarget = kmpExtension.targets.requirePlatform(
                KotlinPlatformType.jvm
            )
            val sourceCollection = jvmTarget.sourceFiles(
                compilationName = KotlinCompilation.MAIN_COMPILATION_NAME
            )

            return JavaCompileInputs(
                sourcePaths = project.files(sourceCollection),
                dependencyClasspath = jvmTarget
                    .compilations[KotlinCompilation.MAIN_COMPILATION_NAME].compileDependencyFiles,
                bootClasspath = project.getAndroidJar()
            )
        }

        // Constructs a JavaCompileInputs from a sourceset
        fun fromSourceSet(sourceSet: SourceSet, project: Project): JavaCompileInputs {
            val sourcePaths: FileCollection = project.files(
                project.provider {
                    sourceSet.allSource.srcDirs
                }
            )
            val dependencyClasspath = sourceSet.compileClasspath
            return JavaCompileInputs(sourcePaths, dependencyClasspath, project.getAndroidJar())
        }

        @Suppress("DEPRECATION") // BaseVariant, SourceKind
        private fun getSourceCollection(
            variant: com.android.build.gradle.api.BaseVariant,
            project: Project
        ): FileCollection {
            // If the project has the kotlin-multiplatform plugin, we want to return a combined
            // collection of all the source files inside '*main' source sets. I.e, given a module
            // with a common and Android source set, this will look inside commonMain and
            // androidMain.
            val taskDependencies = mutableListOf<Any>(variant.javaCompileProvider)
            val sourceFiles = project.multiplatformExtension?.let { kmpExtension ->
                project.provider {
                    kmpExtension.targets.requirePlatform(
                        KotlinPlatformType.androidJvm
                    ).sourceFiles(compilationName = variant.name)
                }
            } ?: project.provider {
                variant
                    .getSourceFolders(com.android.build.gradle.api.SourceKind.JAVA)
                    .map { folder ->
                        for (builtBy in folder.builtBy) {
                            taskDependencies.add(builtBy)
                        }
                        folder.dir
                    }
            }

            val sourceCollection = project.files(sourceFiles)
            for (dep in taskDependencies) {
                sourceCollection.builtBy(dep)
            }
            return sourceCollection
        }

        /**
         * Returns the list of Files (might be directories) that are included in the compilation
         * of this target.
         *
         * @param compilationName The name of the compilation. A target might have separate
         * compilations (e.g. main vs test for jvm or debug vs release for Android)
         */
        private fun KotlinTarget.sourceFiles(
            compilationName: String
        ): List<File> {
            val selectedCompilation = checkNotNull(compilations.findByName(compilationName)) {
                """
                Cannot find $compilationName compilation configuration of $name in
                ${project.parent}.
                Available compilations: ${compilations.joinToString(", ") { it.name }}
                """.trimIndent()
            }
            return selectedCompilation
                .allKotlinSourceSets
                .flatMap {
                    it.kotlin.sourceDirectories
                }.also {
                    require(it.isNotEmpty()) {
                        """
                        Didn't find any source sets for $selectedCompilation in ${project.path}.
                        """.trimIndent()
                    }
                }
        }

        /**
         * Returns the [KotlinTarget] that targets the given platform type.
         *
         * This method will throw if there are no matching targets or there are more than 1 matching
         * target.
         */
        private fun Collection<KotlinTarget>.requirePlatform(
            expectedPlatformType: KotlinPlatformType
        ): KotlinTarget {
            return this.singleOrNull {
                it.platformType == expectedPlatformType
            } ?: error(
                """
                Expected 1 and only 1 kotlin target with $expectedPlatformType. Found $size.
                Matching compilation targets:
                    ${joinToString(",") { it.name }}
                All compilation targets:
                    ${this@requirePlatform.joinToString(",") { it.name }}
                """.trimIndent()
            )
        }
    }
}
