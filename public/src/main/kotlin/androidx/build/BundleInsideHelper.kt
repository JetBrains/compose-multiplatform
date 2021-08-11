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

package androidx.build

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import java.io.File

/**
 * Allow java and Android libraries to bundle other projects inside the project jar/aar.
 */
object BundleInsideHelper {
    /**
     * Creates a configuration for the users to use that will be used to bundle these dependency
     * jars inside of libs/ directory inside of the aar.
     *
     * ```
     * dependencies {
     *   bundleInside(project(":foo"))
     * }
     * ```
     *
     * Used project are expected
     *
     * @see forInsideAar(String, String)
     *
     * @receiver the project that should bundle jars specified by this configuration
     * @param relocations a list of package relocations to apply
     */
    @JvmStatic
    fun Project.forInsideAar(relocations: List<Relocation>) {
        val bundle = configurations.create("bundleInside")
        val repackage = configureRepackageTaskForType(relocations, bundle)
        // Add to AGP's configuration so this jar get packaged inside of the aar.
        dependencies.add("implementation", files(repackage.flatMap { it.archiveFile }))
    }
    /**
     * Creates 3 configurations for the users to use that will be used bundle these dependency
     * jars inside of libs/ directory inside of the aar.
     *
     * ```
     * dependencies {
     *   bundleInside(project(":foo"))
     * }
     * ```
     *
     * Used project are expected
     *
     * @receiver the project that should bundle jars specified by these configurations
     * @param from specifies from which package the rename should happen
     * @param to specifies to which package to put the renamed classes
     */
    @JvmStatic
    fun Project.forInsideAar(from: String, to: String) {
        forInsideAar(listOf(Relocation(from, to)))
    }

    /**
     * Creates a configuration for the users to use that will be used bundle these dependency
     * jars inside of this project's jar.
     *
     * ```
     * dependencies {
     *   bundleInside(project(":foo"))
     *   debugBundleInside(project(path: ":bar", configuration: "someDebugConfiguration"))
     *   releaseBundleInside(project(path: ":bar", configuration: "someReleaseConfiguration"))
     * }
     * ```
     * @receiver the project that should bundle jars specified by these configurations
     * @param from specifies from which package the rename should happen
     * @param to specifies to which package to put the renamed classes
     */
    @JvmStatic
    fun Project.forInsideJar(from: String, to: String) {
        val bundle = configurations.create("bundleInside")
        val repackage = configureRepackageTaskForType(
            listOf(Relocation(from, to)),
            bundle
        )
        dependencies.add("compileOnly", files(repackage.flatMap { it.archiveFile }))
        dependencies.add("testImplementation", files(repackage.flatMap { it.archiveFile }))

        val jarTask = tasks.named("jar")
        jarTask.configure {
            it as Jar
            it.from(repackage.map { files(zipTree(it.archiveFile.get().asFile)) })
        }
        configurations.getByName("apiElements") {
            it.outgoing.artifacts.clear()
            it.outgoing.artifact(
                jarTask.flatMap { jarTask ->
                    jarTask as Jar
                    jarTask.archiveFile
                }
            )
        }
        configurations.getByName("runtimeElements") {
            it.outgoing.artifacts.clear()
            it.outgoing.artifact(
                jarTask.flatMap { jarTask ->
                    jarTask as Jar
                    jarTask.archiveFile
                }
            )
        }
    }

    /**
     * Creates a configuration for users to use that will be used bundle these dependency
     * jars inside of this lint check's jar. This is required because lintPublish does
     * not currently support dependencies, so instead we need to bundle any dependencies with the
     * lint jar manually. (b/182319899)
     *
     * ```
     * dependencies {
     *     if (rootProject.hasProperty("android.injected.invoked.from.ide")) {
     *         compileOnly(LINT_API_LATEST)
     *     } else {
     *         compileOnly(LINT_API_MIN)
     *     }
     *     compileOnly(KOTLIN_STDLIB)
     *     // Include this library inside the resulting lint jar
     *     bundleInside(project(":foo-lint-utils"))
     * }
     * ```
     * @receiver the project that should bundle jars specified by these configurations
     */
    @JvmStatic
    fun Project.forInsideLintJar() {
        val bundle = configurations.create("bundleInside")
        val compileOnly = configurations.getByName("compileOnly")
        val testImplementation = configurations.getByName("testImplementation")
        // bundleInside dependencies should be included as compileOnly as well
        compileOnly.setExtendsFrom(listOf(bundle))
        testImplementation.setExtendsFrom(listOf(bundle))

        tasks.named("jar").configure { jarTask ->
            jarTask as Jar
            jarTask.dependsOn(bundle)
            jarTask.from({
                bundle
                    // The stdlib is already bundled with lint, so no need to include it manually
                    // in the lint.jar if any dependencies here depend on it
                    .filter { !it.name.contains("kotlin-stdlib") }
                    .map { file ->
                        if (file.isDirectory) {
                            file
                        } else {
                            zipTree(file)
                        }
                    }
            })
        }
    }

    data class Relocation(val from: String, val to: String)

    private fun Project.configureRepackageTaskForType(
        relocations: List<Relocation>,
        configuration: Configuration
    ): TaskProvider<ShadowJar> {
        return tasks.register(
            "repackageBundledJars",
            ShadowJar::class.java
        ) { task ->
            task.apply {
                configurations = listOf(configuration)
                for (relocation in relocations) {
                    relocate(relocation.from, relocation.to)
                }
                archiveBaseName.set("repackaged")
                archiveVersion.set("")
                destinationDirectory.set(File(buildDir, "repackaged"))
            }
        }
    }
}
