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
import com.google.common.base.Preconditions
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create

object Jacoco {
    public const val VERSION = "0.8.3"
    const val CORE_DEPENDENCY = "org.jacoco:org.jacoco.core:$VERSION"
    private const val ANT_DEPENDENCY = "org.jacoco:org.jacoco.ant:$VERSION"

    fun createUberJarTask(project: Project): TaskProvider<Jar> {
        // This "uber" jacoco jar is used by the build server. Instrumentation tests are executed
        // outside of Gradle and this is needed to process the coverage files.

        val config = project.configurations.create("myJacoco")
        config.dependencies.add(project.dependencies.create(ANT_DEPENDENCY))

        val task = project.tasks.register("jacocoAntUberJar", Jar::class.java) {
            it.inputs.files(config)
            val resolvedArtifacts = config.resolvedConfiguration.resolvedArtifacts
            it.from(resolvedArtifacts.map { project.zipTree(it.file) }) {
                it.exclude("META-INF/*.SF")
                it.exclude("META-INF/*.DSA")
                it.exclude("META-INF/*.RSA")
            }
            it.destinationDir = project.getDistributionDirectory()
            it.archiveName = "jacocoant.jar"
        }
        return task
    }

    @JvmStatic
    fun registerClassFilesTask(project: Project, provider: TaskProvider<out Task>) {
        project.rootProject.tasks.named("packageAllClassFilesForCoverageReport", Jar::class.java)
            .configure {
                it.from(provider)
            }
    }

    fun createCoverageJarTask(project: Project): TaskProvider<Jar> {
        Preconditions.checkArgument(project.isRoot, "Must be root project")
        // Package the individual *-allclasses.jar files together to generate code coverage reports
        return project.tasks.register(
            "packageAllClassFilesForCoverageReport",
            Jar::class.java
        ) {
            it.destinationDir = project.getDistributionDirectory()
            it.archiveName = "jacoco-report-classes-all.jar"
        }
    }
}
