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

import androidx.build.dependencies.GUAVA_VERSION
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaAndroidTask
import org.jetbrains.dokka.gradle.PackageOptions
import java.io.File
import java.net.URL

object Dokka {
    private fun generatorTaskNameForType(language: String = ""): String {
        val formattedLangauage = language.toLowerCase().capitalize()
        return "dokka${formattedLangauage}Docs"
    }

    fun createDokkaTask(
        project: Project,
        hiddenPackages: List<String>,
        language: String,
        outputFormat: String,
        dacRoot: String
    ): TaskProvider<DokkaAndroidTask> {
        // This function creates and configures a DokkaAndroidTask.
        // The meanings of these parameters are documented at https://github.com/kotlin/dokka

        val docTaskName = generatorTaskNameForType(language)

        val guavaDocLink = createExternalDocumentationLinkMapping(
            "package-lists/guava/package-list",
            "https://guava.dev/releases/$GUAVA_VERSION/api/docs/",
            project
        )
        val kotlinLangLink = createExternalDocumentationLinkMapping(
            "package-lists/kotlin/package-list",
            "https://kotlinlang.org/api/latest/jvm/stdlib/",
            project
        )
        val coroutinesCoreLink = createExternalDocumentationLinkMapping(
            "package-lists/coroutinesCore/package-list",
            "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/",
            project
        )
        val androidLink = createExternalDocumentationLinkMapping(
            "package-lists/android/package-list",
            "https://developer.android.com/reference/",
            project
        )

        return project.tasks.register(docTaskName, DokkaAndroidTask::class.java) { task ->
            task.moduleName = project.name
            task.outputDirectory = File(project.buildDir, docTaskName).absolutePath
            task.description = "Generates $language documentation in the style of " +
                "d.android.com.  Places docs in ${task.outputDirectory}"
            task.outputFormat = outputFormat
            task.outlineRoot = "androidx/"
            task.dacRoot = dacRoot
            task.moduleName = ""
            task.externalDocumentationLinks.add(guavaDocLink)
            task.externalDocumentationLinks.add(kotlinLangLink)
            task.externalDocumentationLinks.add(coroutinesCoreLink)
            task.externalDocumentationLinks.add(androidLink)
            for (hiddenPackage in hiddenPackages) {
                val opts = PackageOptions()
                opts.prefix = hiddenPackage
                opts.suppress = true
                task.perPackageOptions.add(opts)
            }
            // TODO(https://github.com/Kotlin/dokka/issues/508) migrate to 'offline' when it exists
            task.noJdkLink = true
            task.noStdlibLink = true
            task.noAndroidSdkLink = true
            task.dokkaFatJar = "org.jetbrains.dokka:dokka-fatjar:0.9.17-g014"
        }
    }

    /**
     * Documentation for external projects may live in other websites.
     * If we want to link to types defined by those projects, we need to tell Dokka which types
     * are defined in which projects, and where those projects are.
     * We do this by providing a file named package-list for each such project.
     * Each package-list file is called an ExternalDocumentationLink by Dokka.
     * We don't make use of Dokka's ability to automatically download these from the internet because
     * we want the build process to be as deterministic as possible.
     */
    private fun createExternalDocumentationLinkMapping(
        localMappingFileName: String,
        externalUrl: String,
        project: Project
    ): DokkaConfiguration.ExternalDocumentationLink {
        return DokkaConfiguration.ExternalDocumentationLink.Builder().apply {
            this.url = URL(externalUrl)
            this.packageListUrl = File(project.rootDir, "docs-public").toPath()
                .resolve(localMappingFileName).toUri().toURL()
        }.build()
    }
}
