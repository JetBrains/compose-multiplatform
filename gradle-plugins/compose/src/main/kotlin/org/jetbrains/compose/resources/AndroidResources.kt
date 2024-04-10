package org.jetbrains.compose.resources

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.utils.ObservableSet
import javax.inject.Inject

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal fun Project.configureAndroidComposeResources(
    kotlinExtension: KotlinMultiplatformExtension,
    androidExtension: BaseExtension
) {
    // 1) get the Kotlin Android Target Compilation -> [A]
    // 2) get default source set name for the 'A'
    // 3) find the associated Android SourceSet in the AndroidExtension -> [B]
    // 4) get all source sets in the 'A' and add its resources to the 'B'
    kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).all { androidTarget ->
        androidTarget.compilations.all { compilation: KotlinJvmAndroidCompilation ->
            compilation.defaultSourceSet.androidSourceSetInfoOrNull?.let { kotlinAndroidSourceSet ->
                androidExtension.sourceSets
                    .matching { it.name == kotlinAndroidSourceSet.androidSourceSetName }
                    .all { androidSourceSet ->
                        (compilation.allKotlinSourceSets as? ObservableSet<KotlinSourceSet>)?.forAll { kotlinSourceSet ->
                            val preparedComposeResources = getPreparedComposeResourcesDir(kotlinSourceSet)
                            androidSourceSet.resources.srcDirs(preparedComposeResources)

                            //fix for AGP < 8.0
                            //usually 'androidSourceSet.resources.srcDir(preparedCommonResources)' should be enough
                            compilation.androidVariant.processJavaResourcesProvider.configure {
                                it.dependsOn(preparedComposeResources)
                            }
                        }
                    }
            }
        }
    }

    //copy fonts from the compose resources dir to android assets
    val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return
    androidComponents.onVariants { variant ->
        val variantResources = project.files()

        kotlinExtension.targets.withType(KotlinAndroidTarget::class.java).all { androidTarget ->
            androidTarget.compilations.all { compilation: KotlinJvmAndroidCompilation ->
                if (compilation.androidVariant.name == variant.name) {
                    project.logger.info("Configure fonts for variant ${variant.name}")
                    (compilation.allKotlinSourceSets as? ObservableSet<KotlinSourceSet>)?.forAll { kotlinSourceSet ->
                        val preparedComposeResources = getPreparedComposeResourcesDir(kotlinSourceSet)
                        variantResources.from(preparedComposeResources)
                    }
                }
            }
        }

        val copyFonts = registerTask<CopyAndroidFontsToAssetsTask>(
            "copy${variant.name.uppercaseFirstChar()}FontsToAndroidAssets"
        ) {
            from.set(variantResources)
        }
        variant.sources?.assets?.addGeneratedSourceDirectory(
            taskProvider = copyFonts,
            wiredWith = CopyAndroidFontsToAssetsTask::outputDirectory
        )
        //exclude a duplication of fonts in apks
        variant.packaging.resources.excludes.add("**/font*/*")
    }
}

//Copy task doesn't work with 'variant.sources?.assets?.addGeneratedSourceDirectory' API
internal abstract class CopyAndroidFontsToAssetsTask : DefaultTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    abstract val from: Property<FileCollection>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        fileSystem.copy {
            it.includeEmptyDirs = false
            it.from(from)
            it.include("**/font*/*")
            it.into(outputDirectory)
        }
    }
}