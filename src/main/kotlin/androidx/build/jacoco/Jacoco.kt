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

package androidx.build.jacoco

import androidx.build.getDistributionDirectory
import androidx.build.gradle.isRoot
import com.android.build.gradle.TestedExtension
import com.google.common.base.Preconditions
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar

object Jacoco {
    const val VERSION = "0.8.3"
    private const val ANT_DEPENDENCY = "org.jacoco:org.jacoco.ant:$VERSION"

    fun createUberJarTask(project: Project): TaskProvider<Jar> {
        // This "uber" jacoco jar is used by the build server. Instrumentation tests are executed
        // outside of Gradle and this is needed to process the coverage files.
        val config = project.configurations.create("myJacoco")
        config.dependencies.add(project.dependencies.create(ANT_DEPENDENCY))

        return project.tasks.register("jacocoAntUberJar", Jar::class.java) {
            it.inputs.files(config)
            val resolvedArtifacts = config.resolvedConfiguration.resolvedArtifacts
            it.from(resolvedArtifacts.map { project.zipTree(it.file) }) { copySpec ->
                copySpec.exclude("META-INF/*.SF")
                copySpec.exclude("META-INF/*.DSA")
                copySpec.exclude("META-INF/*.RSA")
            }
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("jacocoant.jar")
        }
    }

    fun registerClassFilesTask(project: Project, extension: TestedExtension) {
        extension.testVariants.all { v ->
            if (v.buildType.isTestCoverageEnabled &&
                v.sourceSets.any { it.javaDirectories.isNotEmpty() }) {
                val jarifyTask = project.tasks.register(
                    "package${v.name.capitalize()}ClassFilesForCoverageReport",
                    Jar::class.java
                ) {
                    it.dependsOn(v.testedVariant.javaCompileProvider)
                    // using get() here forces task configuration, but is necessary
                    // to obtain a valid value for destinationDir
                    it.from(v.testedVariant.javaCompileProvider.get().destinationDir)
                    it.exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.class")
                    it.destinationDirectory.set(project.buildDir)
                    it.archiveFileName.set("${project.name}-${v.baseName}-allclasses.jar")
                }
                project.rootProject.tasks.named(
                    "packageAllClassFilesForCoverageReport",
                    Jar::class.java
                ).configure { it.from(jarifyTask) }
            }
        }
    }

    fun createCoverageJarTask(project: Project): TaskProvider<Jar> {
        Preconditions.checkArgument(project.isRoot, "Must be root project")
        // Package the individual *-allclasses.jar files together to generate code coverage reports
        return project.tasks.register(
            "packageAllClassFilesForCoverageReport",
            Jar::class.java
        ) {
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("jacoco-report-classes-all.jar")
        }
    }
}
