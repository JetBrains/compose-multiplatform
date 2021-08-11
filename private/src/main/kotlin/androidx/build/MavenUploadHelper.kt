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

package androidx.build

import androidx.build.AndroidXComposePlugin.Companion.isMultiplatformEnabled
import com.android.build.gradle.LibraryPlugin
import groovy.util.Node
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import java.io.File

fun Project.configureMavenArtifactUpload(extension: AndroidXExtension) {
    apply(mapOf("plugin" to "maven-publish"))

    afterEvaluate {
        components.all { component ->
            configureComponent(extension, component)
        }
    }
}

private fun Project.configureComponent(
    extension: AndroidXExtension,
    component: SoftwareComponent
) {
    if (extension.publish.shouldPublish() && component.isAndroidOrJavaReleaseComponent()) {
        val androidxGroup = validateCoordinatesAndGetGroup(extension)
        group = androidxGroup
        configure<PublishingExtension> {
            repositories {
                it.maven { repo ->
                    repo.setUrl(getRepositoryDirectory())
                }
            }
            publications {
                if (appliesJavaGradlePluginPlugin()) {
                    // The 'java-gradle-plugin' will also add to the 'pluginMaven' publication
                    it.create<MavenPublication>("pluginMaven")
                    tasks.getByName("publishPluginMavenPublicationToMavenRepository").doFirst {
                        removePreviouslyUploadedArchives(androidxGroup)
                    }
                } else {
                    it.create<MavenPublication>("maven") {
                        from(component)
                    }
                    tasks.getByName("publishMavenPublicationToMavenRepository").doFirst {
                        removePreviouslyUploadedArchives(androidxGroup)
                    }
                }
            }
            publications.withType(MavenPublication::class.java).all {
                it.pom { pom ->
                    addInformativeMetadata(extension, pom)
                    tweakDependenciesMetadata(extension, pom)
                }
            }
        }

        // Register it as part of release so that we create a Zip file for it
        Release.register(this, extension)

        // Workaround for https://github.com/gradle/gradle/issues/11717
        project.tasks.withType(GenerateModuleMetadata::class.java).configureEach { task ->
            task.doLast {
                val metadata = task.outputFile.asFile.get()
                var text = metadata.readText()
                metadata.writeText(
                    text.replace(
                        "\"buildId\": .*".toRegex(),
                        "\"buildId:\": \"${getBuildId()}\""
                    )
                )
            }
        }

        if (isMultiplatformEnabled()) {
            configureMultiplatformPublication()
        }
    }
}

private fun Project.configureMultiplatformPublication() {
    val multiplatformExtension = extensions.findByType<KotlinMultiplatformExtension>() ?: return

    // publishMavenPublicationToMavenRepository will produce conflicting artifacts with the same
    // name as the artifacts producing by publishKotlinMultiplatformPublicationToMavenRepository
    project.tasks.findByName("publishMavenPublicationToMavenRepository")?.enabled = false

    multiplatformExtension.targets.all { target ->
        if (target is KotlinAndroidTarget) {
            target.publishAllLibraryVariants()
        }
    }
}

private fun SoftwareComponent.isAndroidOrJavaReleaseComponent() =
    name == "release" || name == "java"

private fun Project.validateCoordinatesAndGetGroup(extension: AndroidXExtension): String {
    val mavenGroup = extension.mavenGroup?.group
        ?: throw Exception("You must specify mavenGroup for $name project")
    val strippedGroupId = mavenGroup.substringAfterLast(".")
    if (mavenGroup.startsWith("androidx") && !name.startsWith(strippedGroupId)) {
        throw Exception("Your artifactId must start with '$strippedGroupId'. (currently is $name)")
    }
    return mavenGroup
}

/**
 * Delete any existing archives, so that developers don't get
 * confused/surprised by the presence of old versions.
 * Additionally, deleting old versions makes it more convenient to iterate
 * over all existing archives without visiting archives having old versions too
 */
private fun Project.removePreviouslyUploadedArchives(group: String) {
    val projectArchiveDir = File(
        getRepositoryDirectory(),
        "${group.replace('.', '/')}/${project.name}"
    )
    projectArchiveDir.deleteRecursively()
}

private fun Project.addInformativeMetadata(extension: AndroidXExtension, pom: MavenPom) {
    pom.name.set(provider { extension.name })
    pom.description.set(provider { extension.description })
    pom.url.set(
        provider {
            fun defaultUrl() = "https://developer.android.com/jetpack/androidx/releases/" +
                extension.mavenGroup!!.group.removePrefix("androidx.")
                    .replace(".", "-") +
                "#" + extension.project.version()
            getAlternativeProjectUrl() ?: defaultUrl()
        }
    )
    pom.inceptionYear.set(provider { extension.inceptionYear })
    pom.licenses { licenses ->
        licenses.license { license ->
            license.name.set("The Apache Software License, Version 2.0")
            license.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            license.distribution.set("repo")
        }
        for (extraLicense in extension.getLicenses()) {
            licenses.license { license ->
                license.name.set(provider { extraLicense.name })
                license.url.set(provider { extraLicense.url })
                license.distribution.set("repo")
            }
        }
    }
    pom.scm { scm ->
        scm.url.set("https://cs.android.com/androidx/platform/frameworks/support")
        scm.connection.set(ANDROID_GIT_URL)
    }
    pom.developers { devs ->
        devs.developer { dev ->
            dev.name.set("The Android Open Source Project")
        }
    }
}

private fun Project.tweakDependenciesMetadata(extension: AndroidXExtension, pom: MavenPom) {
    pom.withXml { xml ->
        // The following code depends on getProjectsMap which is only available late in
        // configuration at which point Java Library plugin's variants are not allowed to be
        // modified. TODO remove the use of getProjectsMap and move to earlier configuration.
        // For more context see:
        // https://android-review.googlesource.com/c/platform/frameworks/support/+/1144664/8/buildSrc/src/main/kotlin/androidx/build/MavenUploadHelper.kt#177
        assignSingleVersionDependenciesInGroupForPom(xml, extension)
        assignAarTypes(xml)
    }
}

// TODO(aurimas): remove this when Gradle bug is fixed.
// https://github.com/gradle/gradle/issues/3170
private fun Project.assignAarTypes(xml: XmlProvider) {
    val androidxDependencies = HashSet<Dependency>()
    collectDependenciesForConfiguration(androidxDependencies, "api")
    collectDependenciesForConfiguration(androidxDependencies, "implementation")
    collectDependenciesForConfiguration(androidxDependencies, "compile")

    val dependencies = xml.asNode().children().find {
        it is Node && it.name().toString().endsWith("dependencies")
    } as Node?

    dependencies?.children()?.forEach { dep ->
        if (dep !is Node) {
            return@forEach
        }
        val groupId = dep.children().first {
            it is Node && it.name().toString().endsWith("groupId")
        } as Node
        val artifactId = dep.children().first {
            it is Node && it.name().toString().endsWith("artifactId")
        } as Node
        if (isAndroidProject(
                groupId.children()[0] as String,
                artifactId.children()[0] as String, androidxDependencies
            )
        ) {
            dep.appendNode("type", "aar")
        }
    }
}

/**
 * Modifies the given .pom to specify that every dependency in <group> refers to a single version
 * and can't be automatically promoted to a new version.
 * This will replace, for example, a version string of "1.0" with a version string of "[1.0]"
 *
 * Note: this is not enforced in Gradle nor in plain Maven (without the Enforcer plugin)
 * (https://github.com/gradle/gradle/issues/8297)
 */
private fun assignSingleVersionDependenciesInGroupForPom(
    xml: XmlProvider,
    extension: AndroidXExtension
) {
    val group = extension.mavenGroup
    if (group == null || !group.requireSameVersion) {
        return
    }

    val dependencies = xml.asNode().children().find {
        it is Node && it.name().toString().endsWith("dependencies")
    } as Node?
    dependencies?.children()?.forEach { dep ->
        if (dep !is Node) {
            return@forEach
        }
        val groupId = dep.children().first {
            it is Node && it.name().toString().endsWith("groupId")
        } as Node
        if (groupId.children()[0].toString() == group.group) {
            val versionNode = dep.children().first {
                it is Node && it.name().toString().endsWith("version")
            } as Node
            val declaredVersion = versionNode.children()[0].toString()
            if (isVersionRange(declaredVersion)) {
                throw GradleException(
                    "Unsupported version '$declaredVersion': " +
                        "already is a version range"
                )
            }
            val pinnedVersion = "[$declaredVersion]"
            versionNode.setValue(pinnedVersion)
        }
    }
}

private fun isVersionRange(text: String): Boolean {
    return text.contains("[") ||
        text.contains("]") ||
        text.contains("(") ||
        text.contains(")") ||
        text.contains(",")
}

private fun Project.collectDependenciesForConfiguration(
    androidxDependencies: MutableSet<Dependency>,
    name: String
) {
    val config = configurations.findByName(name)
    config?.dependencies?.forEach { dep ->
        if (dep.group?.startsWith("androidx.") == true) {
            androidxDependencies.add(dep)
        }
    }
}

private fun Project.isAndroidProject(
    groupId: String,
    artifactId: String,
    deps: Set<Dependency>
): Boolean {
    for (dep in deps) {
        if (dep is ProjectDependency) {
            if (dep.group == groupId && dep.name == artifactId) {
                return dep.dependencyProject.plugins.hasPlugin(LibraryPlugin::class.java)
            }
        }
    }
    val projectModules = project.getProjectsMap()
    projectModules["$groupId:$artifactId"]?.let { module ->
        return project.findProject(module)?.plugins?.hasPlugin(LibraryPlugin::class.java) ?: false
    }
    return false
}

private fun Project.appliesJavaGradlePluginPlugin() = pluginManager.hasPlugin("java-gradle-plugin")

private const val ANDROID_GIT_URL =
    "scm:git:https://android.googlesource.com/platform/frameworks/support"
