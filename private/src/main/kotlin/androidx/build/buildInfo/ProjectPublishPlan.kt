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

package androidx.build.buildInfo

import androidx.build.AndroidXExtension
import androidx.build.LibraryGroup
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Information extracted from a gradle project and [AndroidXExtension] that creates a view on
 * which configurations will be published, so that build_info files can parallel the maven
 * publications and contain the same dependencies
 */
data class ProjectPublishPlan(
    val shouldRelease: Boolean,
    val mavenGroup: LibraryGroup?,
    val variants: List<VariantPublishPlan>
) {
    /**
     * Info about a particular variant that will be published
     *
     * @param artifactId the maven artifact id
     * @param taskSuffix if non-null, will be added to the end of task names to disambiguate
     *                   (i.e. createLibraryBuildInfoFiles becomes createLibraryBuildInfoFilesJvm)
     * @param configurationName name of the configuration containing the dependencies for this
     *                          variant
     */
    data class VariantPublishPlan(
        val artifactId: String,
        val taskSuffix: String? = null,
        val configurationName: String = "releaseRuntimeElements"
    )
}

/**
 * Compute what groups and variants will be published for this project.
 *
 * Soon, we believe we should switch this to being based directly on the publication objects,
 * rather than the underlying Configurations (b/238762087).
 */
fun AndroidXExtension.computePublishPlan(): ProjectPublishPlan {
    return ProjectPublishPlan(
        shouldRelease = shouldRelease(),
        mavenGroup = mavenGroup,
        variants = project.configurations.filter { project.shouldBeRepresentedInBuildInfo(it) }
            .map { config ->
                ProjectPublishPlan.VariantPublishPlan(
                    artifactId = config.outgoing.artifacts.single().name,
                    taskSuffix = config.getKotlinTargetName()?.replaceFirstChar { it.uppercase() },
                    configurationName = config.name
                )
            })
}

private fun Configuration.getKotlinTargetName() =
    attributes.getAttribute(KotlinPlatformType.attribute)?.name

// TODO(b/237688690): understand where -published is coming from, and have a more principled reason
//   for excluding it, or better strategy for including it
/*
 * Pre-KMP, we assumed that "runtimeElements" was the only configuration whose dependencies needed
 * to be tracked by JetPad.  With KMP, we need a more generic way of choosing out gradle
 * configurations.
 */
private fun Project.shouldBeRepresentedInBuildInfo(config: Configuration) =
    isJavaRuntime(config) &&
        isLibrary(config) &&
        !isShadowed(config) &&
        config.artifacts.size == 1 &&
        !(config.name.endsWith("-published"))

private fun Project.isLibrary(config: Configuration) =
    config.getCategory() == objects.named<Category>(Category.LIBRARY)

private fun Project.isJavaRuntime(config: Configuration) =
    config.getUsage() == objects.named<Usage>(Usage.JAVA_RUNTIME)

private fun Project.isShadowed(config: Configuration) =
    config.getBundling() == objects.named<Bundling>(Bundling.SHADOWED)

private fun Configuration.getCategory() = attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
private fun Configuration.getUsage() = attributes.getAttribute(Usage.USAGE_ATTRIBUTE)
private fun Configuration.getBundling() = attributes.getAttribute(Bundling.BUNDLING_ATTRIBUTE)