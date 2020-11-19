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
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import java.io.File

/**
 * Allow java and Android libraries to bundle other projects inside the project jar/aar.
 */
object BundleInsideHelper {
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
        val bundle = configurations.create("bundleInside")
        val bundleDebug = configurations.create("debugBundleInside") {
            it.extendsFrom(bundle)
        }
        val bundleRelease = configurations.create("releaseBundleInside") {
            it.extendsFrom(bundle)
        }
        val repackageRelease = configureRepackageTaskForType("Release", from, to, bundleRelease)
        val repackageDebug = configureRepackageTaskForType("Debug", from, to, bundleDebug)

        // Add to AGP's configurations so these jars get packaged inside of the aar.
        dependencies.add(
            "releaseImplementation",
            files(repackageRelease.flatMap { it.archiveFile })
        )
        dependencies.add("debugImplementation", files(repackageDebug.flatMap { it.archiveFile }))

        // Android lint is silly (b/173445333), force build both debug and release
        tasks.withType(JavaCompile::class.java).configureEach { task ->
            task.dependsOn(repackageDebug)
            task.dependsOn(repackageRelease)
        }
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
        val repackage = configureRepackageTaskForType("jar", from, to, bundle)
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

    private fun Project.configureRepackageTaskForType(
        type: String,
        from: String,
        to: String,
        configuration: Configuration
    ): TaskProvider<ShadowJar> {
        return tasks.register(
            "repackageBundledJars$type",
            ShadowJar::class.java
        ) { task ->
            task.apply {
                configurations = listOf(configuration)
                relocate(from, to)
                archiveBaseName.set("repackaged-$type")
                archiveVersion.set("")
                destinationDirectory.set(File(buildDir, "repackaged"))
            }
        }
    }
}
