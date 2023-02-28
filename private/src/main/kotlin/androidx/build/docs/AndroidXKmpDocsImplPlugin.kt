/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.docs

import androidx.build.dokka.kmpDocs.DokkaCombinedDocsTask
import androidx.build.dokka.kmpDocs.DokkaPartialDocsTask
import androidx.build.getDistributionDirectory
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Plugin that sets up tasks to generate offline docs via Dokka.
 */
class AndroidXKmpDocsImplPlugin @Inject constructor(
    private val objectFactory: ObjectFactory
) : Plugin<Project> {
    override fun apply(project: Project) {
        val config = project.configurations.register(CONSUMER_CONFIGURATION_NAME) { config ->
            config.configureKmpDocsAttributes(objectFactory)
        }
        val combinedDocsTask = DokkaCombinedDocsTask.register(
            project,
            config
        )
        project.tasks.register(ZIP_COMBINED_DOCS_TASK_NAME, Zip::class.java) {
            it.from(
                combinedDocsTask.map { it.outputDir }
            )
            it.destinationDirectory.set(
                project.getDistributionDirectory().resolve("kmp-docs")
            )
            it.archiveBaseName.set(
                "kmpDocs"
            )
        }
    }

    companion object {
        private const val CONSUMER_CONFIGURATION_NAME = "kmpDocs"
        private const val PRODUCER_CONFIGURATION_NAME = "partialKmpDocs"
        private const val ATTRIBUTE_NAME = "partial-docs"
        private const val ZIP_COMBINED_DOCS_TASK_NAME = "zipCombinedKmpDocs"

        /**
         * Adds ability to generates partial dokka docs artifact for the Kotlin project.
         */
        fun setupPartialDocsArtifact(
            project: Project
        ) {
            val kotlinExtension = project.the<KotlinProjectExtension>()
            val docsWithPlaceholderSourceLinks = DokkaPartialDocsTask.register(
                project,
                kotlinExtension
            )
            project.configurations.create(PRODUCER_CONFIGURATION_NAME) { config ->
                config.configureKmpDocsAttributes(project.objects)
                config.isCanBeResolved = false
                config.isCanBeConsumed = true
            }
            project.artifacts { artifactHandler ->
                artifactHandler.add(
                    PRODUCER_CONFIGURATION_NAME,
                    docsWithPlaceholderSourceLinks.map {
                        it.outputDir
                    })
            }
        }

        private fun Configuration.configureKmpDocsAttributes(objectFactory: ObjectFactory) {
            attributes {
                it.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objectFactory.named(ATTRIBUTE_NAME)
                )
            }
        }
    }
}