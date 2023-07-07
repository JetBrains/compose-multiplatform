/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.inspection.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
/**
 * Task purposely empty, unused class that would be removed by proguard. See javadoc below for more
 * information.
 */
@Suppress("UnstableApiUsage")
@DisableCachingByDefault(because = "Simply generates a small file and doesn't benefit from caching")
abstract class GenerateProguardDetectionFileTask : DefaultTask() {

    @get:Input
    abstract val mavenGroup: Property<String>

    @get:Input
    abstract val mavenArtifactId: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateProguardDetectionFile() {
        val packageName = generatePackageName(mavenGroup.get(), mavenArtifactId.get())
        val path = packageName.replace('.', '/')
        val dir = File(outputDir.get().asFile, path)
        if (!dir.exists() && !dir.mkdirs()) {
            throw GradleException("Failed to create directory $dir")
        }
        val file = File(dir, "ProguardDetection.kt")
        logger.debug("Generating ProguardDetection in $dir")

        val text = """
            package $packageName;
            
            /**
             * Purposely empty, unused class that would be removed by proguard.
             *
             * We use this class to detect if a target app was proguarded or not, because if it was,
             * it likely means we can't reliably inspect it, since library's methods used only by
             * an inspector would get removed. Instead, we'll need to tell users that app
             * inspection isn't available for the current app and that they should rebuild it
             * again without proguarding to continue.
             */
             private class ProguardDetection {}
        """.trimIndent()

        file.writeText(text)
    }
}

@ExperimentalStdlibApi
@Suppress("DEPRECATION") // BaseVariant
fun Project.registerGenerateProguardDetectionFileTask(
    variant: com.android.build.gradle.api.BaseVariant
) {
    val outputDir = taskWorkingDir(variant, "generateProguardDetection")
    val taskName = variant.taskName("generateProguardDetection")
    val mavenGroup = project.group as? String
        ?: throw GradleException("MavenGroup must be specified")
    val mavenArtifactId = project.name
    val task = tasks.register(taskName, GenerateProguardDetectionFileTask::class.java) {
        it.outputDir.set(outputDir)
        it.mavenGroup.set(mavenGroup)
        it.mavenArtifactId.set(mavenArtifactId)
    }
    variant.registerJavaGeneratingTask(task, outputDir)
}

/**
 * Produces package name for `ProguardDetection` class, e.g for following params:
 * mavenGroup: androidx.work, mavenArtifact: work-runtime, result will be:
 * androidx.inspection.work.runtime.
 *
 * The file is specifically generated in package androidx.inspection, so keep rules like
 * "androidx.work.*" won't keep this file, because keeping library itself is not enough.
 * Inspectors could use API dependencies of the inspected library, so if those API
 * dependencies are renamed / minified that can break an inspector.
 */
internal fun generatePackageName(mavenGroup: String, mavenArtifact: String): String {
    val strippedArtifact = mavenArtifact.removePrefix(mavenGroup.split('.').last())
        .removePrefix("-").replace('-', '.')
    val group = mavenGroup.removePrefix("androidx.")
    // It's possible for strippedArtifact to be empty, e.g. "compose.ui/ui" has no hyphen
    return "androidx.inspection.$group" +
        if (strippedArtifact.isNotEmpty()) ".$strippedArtifact" else ""
}
