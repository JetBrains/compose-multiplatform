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

// This file creates tasks for generating documentation from source code using Dokka
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from DokkaSourceDocs to just SourceDocs
package androidx.build.dokka

import androidx.build.AndroidXExtension
import androidx.build.defaultPublishVariant
import androidx.build.java.JavaCompileInputs
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaTask

object DokkaSourceDocs {
    private const val DOCS_TYPE = "TipOfTree"
    public val ARCHIVE_TASK_NAME: String = Dokka.archiveTaskNameForType(DOCS_TYPE)
    // TODO(b/72330103) make "generateDocs" be the only archive task once Doclava is fully removed
    private val ALTERNATE_ARCHIVE_TASK_NAME: String = "generateDocs"

    private val hiddenPackages = DokkaPublicDocs.hiddenPackages

    fun tryGetRunnerProject(project: Project): Project? {
        return project.rootProject.findProject(":docs-runner")
    }

    fun getRunnerProject(project: Project): Project {
        return tryGetRunnerProject(project)!!
    }

    fun getDocsTasks(project: Project): TaskCollection<DokkaTask>? {
        val runnerProject = getRunnerProject(project)
        val docsTasks = runnerProject.tasks.getOrCreateDocsTask(runnerProject)
        return docsTasks
    }

    @Synchronized fun TaskContainer.getOrCreateDocsTask(runnerProject: Project):
            TaskCollection<DokkaTask> {
        val tasks = this
        var dokkaTasks = runnerProject.tasks.withType(DokkaTask::class.java)
            .matching { it.name.contains(DOCS_TYPE) }

        if (dokkaTasks.isEmpty()) {
            Dokka.createDocsTask(DOCS_TYPE, runnerProject, hiddenPackages)
            dokkaTasks = runnerProject.tasks.withType(DokkaTask::class.java)
                .matching { it.name.contains(DOCS_TYPE) }

            if (tasks.findByName(ALTERNATE_ARCHIVE_TASK_NAME) == null) {
                tasks.register(ALTERNATE_ARCHIVE_TASK_NAME) {
                    it.dependsOn(tasks.named(ARCHIVE_TASK_NAME))
                }
            }
        }
        return dokkaTasks
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        if (tryGetRunnerProject(project) == null) {
            return
        }
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        library.defaultPublishVariant { variant ->
            project.afterEvaluate {
                val inputs = JavaCompileInputs.fromLibraryVariant(library, variant, project)
                registerInputs(inputs, project)
            }
        }
    }

    fun registerJavaProject(
        project: Project,
        extension: AndroidXExtension
    ) {
        if (tryGetRunnerProject(project) == null) {
            return
        }
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
        project.afterEvaluate {
            val inputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
            registerInputs(inputs, project)
        }
    }

    fun registerInputs(inputs: JavaCompileInputs, project: Project) {
        val dokkaTasks = getDocsTasks(project)

        // Avoid depending on or modifying a task that has already been executed.
        // Because registerInputs is called in an afterEvaluate, we can end up in a case where the dokka tasks are
        // cached and have been executed in a prior run
        dokkaTasks?.filter { it.state.isConfigurable }?.forEach {
            it.sourceDirs += inputs.sourcePaths

            it.classpath = project.files(it.classpath).plus(project.files(inputs.bootClasspath))
                .plus(inputs.dependencyClasspath)
            it.dependsOn(inputs.dependencyClasspath)
        }
    }
}
