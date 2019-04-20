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

// This file creates tasks for generating documentation using Dokka
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from Dokka to just Docs
package androidx.build.dokka

import androidx.build.AndroidXExtension
import androidx.build.DiffAndDocs
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.PackageOptions
import java.io.File

object Dokka {
    fun generatorTaskNameForType(docsType: String): String {
        return "dokka${docsType}Docs"
    }
    fun archiveTaskNameForType(docsType: String): String {
        return "dist${docsType}DokkaDocs"
    }
    fun createDocsTask(
        docsType: String, // "public" or "tipOfTree"
        project: Project,
        hiddenPackages: List<String>
    ) {
        val taskName = generatorTaskNameForType(docsType)
        val archiveTaskName = archiveTaskNameForType(docsType)
        project.apply<DokkaAndroidPlugin>()
        // We don't use the `dokka` task, but it normally appears in `./gradlew tasks`
        // so replace it with a new task that doesn't show up and doesn't do anything
        project.tasks.replace("dokka")
        if (project.name != "support" && project.name != "docs-runner") {
            throw Exception("Illegal project passed to createDocsTask: " + project.name)
        }
        val docsTask = project.tasks.create(taskName, DokkaAndroidTask::class.java) { docsTask ->
            docsTask.moduleName = project.name
            docsTask.outputDirectory = File(project.buildDir, taskName).absolutePath
            docsTask.description = "Generates $docsType Kotlin documentation in the style of " +
                    "d.android.com.  Places docs in ${docsTask.outputDirectory}"
            docsTask.outputFormat = "dac"
            docsTask.outlineRoot = "androidx/"
            docsTask.dacRoot = "/reference/kotlin"
            docsTask.moduleName = ""
            for (hiddenPackage in hiddenPackages) {
                val opts = PackageOptions()
                opts.prefix = hiddenPackage
                opts.suppress = true
                docsTask.perPackageOptions.add(opts)
            }
        }

        project.tasks.create(archiveTaskName, Zip::class.java) { zipTask ->
            zipTask.dependsOn(docsTask)
            zipTask.from(docsTask.outputDirectory) { copySpec ->
                copySpec.into("reference/kotlin")
            }
            zipTask.archiveBaseName.set(taskName)
            zipTask.archiveVersion.set(getBuildId())
            zipTask.destinationDirectory.set(project.getDistributionDirectory())
            zipTask.description = "Zips $docsType Kotlin documentation (generated via " +
                "Dokka in the style of d.android.com) into ${zipTask.archivePath}"
            zipTask.group = JavaBasePlugin.DOCUMENTATION_GROUP
        }
    }

    fun Project.configureAndroidProjectForDokka(
        library: LibraryExtension,
        extension: AndroidXExtension
    ) {
        afterEvaluate {
            if (name != "docs-runner") {
                DiffAndDocs.get(this).registerAndroidProject(library, extension)
            }

            DokkaPublicDocs.registerProject(this, extension)
            DokkaSourceDocs.registerAndroidProject(this, library, extension)
        }
    }

    fun Project.configureJavaProjectForDokka(extension: AndroidXExtension) {
        afterEvaluate {
            if (name != "docs-runner") {
                DiffAndDocs.get(this).registerJavaProject(this, extension)
            }
            DokkaPublicDocs.registerProject(this, extension)
            DokkaSourceDocs.registerJavaProject(this, extension)
        }
    }
}
