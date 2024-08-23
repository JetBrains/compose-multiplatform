package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

//configure single module resources (no publishing, no module isolation)
internal fun Project.configureSinglemoduleResources(
    kotlinExtension: KotlinProjectExtension,
    resClassSourceSetName: String,
    config: Provider<ResourcesExtension>
) {
    logger.info("Configure compose resources")
    configureComposeResourcesGeneration(kotlinExtension, resClassSourceSetName, config, false)

    // mark prepared resources as sourceSet.resources
    // 1) it automatically packs the resources to JVM jars
    // 2) it configures the webpack to use the resources
    // 3) for native targets we will use source set resources to pack them into the final app. see IosResources.kt
    // 4) for the android it DOESN'T pack resources! we copy resources to assets in AndroidResources.kt
    kotlinExtension.sourceSets.all { sourceSet ->
        // the HACK is here because KGP copy androidMain java resources to Android target
        // if the resources were registered in the androidMain source set before the target declaration
        afterEvaluate {
            sourceSet.resources.srcDirs(getPreparedComposeResourcesDir(sourceSet))
        }
    }

    onAgpApplied {
        configureAndroidComposeResources()
        fixAndroidLintTaskDependencies()
    }
}