/*
 * Copyright 2021 The Android Open Source Project
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

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import java.lang.reflect.Modifier
import java.util.concurrent.Callable

object ComposeJvmTarget {
    /**
     * Temporary workaround for fixing Compose Desktop build with Kotlin 1.4 and Gradle 7
     * Intended to be used only in :compose:desktop:desktop and only until Compose project
     * switches to Kotlin 1.5.0
     *
     * This is basically a copy
     * org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget.withJava from Kotlin 1.5.0,
     * which fixes java.lang.NoSuchMethodError, thrown by Gradle 7 with Kotlin 1.4
     * (https://youtrack.jetbrains.com/issue/KTIJ-10018).
     *
     * There are a few changes from the original:
     * some internal methods are copied or called using Java Reflection
     */
    @JvmStatic
    fun withJava(target: KotlinJvmTarget) {
        val project = target.project

        project.plugins.apply(JavaPlugin::class.java)
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        setUpJavaSourceSets(target)

        javaPluginConvention.sourceSets.all { javaSourceSet ->
            val compilation = target.compilations.getByName(javaSourceSet.name)
            val compileJavaTask = project.tasks.withType(AbstractCompile::class.java)
                .named(javaSourceSet.compileJavaTaskName)

            setupJavaSourceSetSourcesAndResources(project, javaSourceSet, compilation)

            val javaClasses = project.files(compileJavaTask.map { it.destinationDir })

            compilation.output.classesDirs.from(javaClasses)

            (javaSourceSet.output.classesDirs as? ConfigurableFileCollection)?.from(
                compilation.output.classesDirs.minus(javaClasses)
            )

            javaSourceSet.output.setResourcesDir(
                Callable { compilation.output.resourcesDirProvider }
            )

            setupDependenciesCrossInclusionForJava(project, compilation, javaSourceSet)
        }

        // Eliminate the Java output configurations from dependency resolution
        // to avoid ambiguity between them and the equivalent configurations
        // created for the target:
        listOf(
            JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME,
            JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME
        ).forEach { outputConfigurationName ->
            project.configurations.findByName(outputConfigurationName)?.isCanBeConsumed = false
        }

        disableJavaPluginTasks(project, javaPluginConvention, target)
    }

    private fun setupDependenciesCrossInclusionForJava(
        project: Project,
        compilation: KotlinJvmCompilation,
        javaSourceSet: SourceSet
    ) {
        // Make sure Kotlin compilation dependencies appear in the Java source set classpaths:

        listOfNotNull(
            compilation.apiConfigurationName,
            compilation.implementationConfigurationName,
            compilation.compileOnlyConfigurationName
        ).forEach { configurationName ->
            project.addExtendsFromRelation(
                javaSourceSet.compileClasspathConfigurationName,
                configurationName
            )
        }

        listOfNotNull(
            compilation.apiConfigurationName,
            compilation.implementationConfigurationName,
            compilation.runtimeOnlyConfigurationName
        ).forEach { configurationName ->
            project.addExtendsFromRelation(
                javaSourceSet.runtimeClasspathConfigurationName,
                configurationName
            )
        }

        // Add the Java source set dependencies to the Kotlin compilation
        // compile & runtime configurations:
        listOfNotNull(
            javaSourceSet.compileOnlyConfigurationName,
            javaSourceSet.apiConfigurationName
                .takeIf { project.configurations.findByName(it) != null },
            javaSourceSet.implementationConfigurationName
        ).forEach { configurationName ->
            project.addExtendsFromRelation(
                compilation.compileDependencyConfigurationName,
                configurationName
            )
        }

        listOfNotNull(
            javaSourceSet.runtimeOnlyConfigurationName,
            javaSourceSet.apiConfigurationName
                .takeIf { project.configurations.findByName(it) != null },
            javaSourceSet.implementationConfigurationName
        ).forEach { configurationName ->
            project.addExtendsFromRelation(
                compilation.runtimeDependencyConfigurationName,
                configurationName
            )
        }
    }

    /**
     * Calls AbstractKotlinPlugin.setUpJavaSourceSets(target, false) using reflection
     */
    private fun setUpJavaSourceSets(target: KotlinJvmTarget) {
        val abstractKotlinPluginClass = Class.forName(ABSTRACT_KOTLIN_PLUGIN)
            ?: error("Could not find '$ABSTRACT_KOTLIN_PLUGIN' class")
        val companionField = abstractKotlinPluginClass.fields
            .find { it.name == COMPANION && Modifier.isStatic(it.modifiers) }
            ?: error("Could not find '$COMPANION' field")
        val companionInstance = companionField.get(abstractKotlinPluginClass)!!
        val companionClass = companionInstance.javaClass
        val setUpJavaSourceSetsMethod = companionClass.methods.find {
            it.name == SET_UP_JAVA_SOURCE_SETS && it.parameterCount == 2
        } ?: error("Could not find '$SET_UP_JAVA_SOURCE_SETS' method")
        setUpJavaSourceSetsMethod.invoke(companionInstance, target, false)
    }

    private fun disableJavaPluginTasks(
        project: Project,
        javaPluginConvention: JavaPluginConvention,
        target: KotlinJvmTarget
    ) {
        // A 'normal' build should not do redundant job like running the tests twice or building two JARs,
        // so disable some tasks and just make them depend on the others:
        val targetJar = project.tasks.withType(Jar::class.java).named(target.artifactsTaskName)

        val mainJarTaskName = javaPluginConvention.sourceSets.getByName("main").jarTaskName
        project.tasks.withType(Jar::class.java).named(mainJarTaskName) { javaJar ->
            (javaJar.source as? ConfigurableFileCollection)?.setFrom(targetJar.map { it.source })
            javaJar.conventionMapping("archiveName") { targetJar.get().archiveFileName.get() }
            javaJar.dependsOn(targetJar)
            javaJar.enabled = false
        }

        project.tasks.withType(Test::class.java).named(JavaPlugin.TEST_TASK_NAME) { javaTestTask ->
            javaTestTask.dependsOn(project.tasks.named(target.testTaskName))
            javaTestTask.enabled = false
        }
    }

    private fun setupJavaSourceSetSourcesAndResources(
        project: Project,
        javaSourceSet: SourceSet,
        compilation: KotlinJvmCompilation
    ) {
        javaSourceSet.java.setSrcDirs(listOf("src/${compilation.defaultSourceSet.name}/java"))
        compilation.defaultSourceSet.kotlin.srcDirs(javaSourceSet.java.sourceDirectories)

        // To avoid confusion in the sources layout, remove the default Java source directories
        // (like src/main/java, src/test/java) and instead add sibling directories to those where the Kotlin
        // sources are placed (i.e. src/jvmMain/java, src/jvmTest/java):
        javaSourceSet.resources.setSrcDirs(
            compilation.defaultSourceSet.resources.sourceDirectories
        )
        compilation.defaultSourceSet.resources.srcDirs(javaSourceSet.resources.sourceDirectories)

        // Resources processing is done with the Kotlin resource processing task:
        val processJavaResourcesTask =
            project.tasks.getByName(javaSourceSet.processResourcesTaskName)
        processJavaResourcesTask.dependsOn(
            project.tasks.getByName(compilation.processResourcesTaskName)
        )
        processJavaResourcesTask.enabled = false
    }

    private const val ABSTRACT_KOTLIN_PLUGIN =
        "org.jetbrains.kotlin.gradle.plugin.AbstractKotlinPlugin"
    private const val COMPANION = "Companion"
    private const val SET_UP_JAVA_SOURCE_SETS = "setUpJavaSourceSets${'$'}kotlin_gradle_plugin"
    private const val ARTIFACT_TASK_NAME = "jar"

    private val KotlinTarget.testTaskName: String
        get() = lowerCamelCaseName(name, "test")

    private fun lowerCamelCaseName(vararg nameParts: String?): String {
        val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
        return nonEmptyParts.drop(1).joinToString(
            separator = "",
            prefix = nonEmptyParts.firstOrNull().orEmpty(),
            transform = String::capitalize
        )
    }
}