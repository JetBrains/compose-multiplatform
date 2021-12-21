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

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.gradle.api.Project
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import kotlin.reflect.full.memberProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.internal.dependencies.DefaultMavenDependency
import org.gradle.api.publish.maven.internal.dependencies.MavenDependencyInternal
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.attributes.Usage


open class AndroidXComposeMultiplatformExtensionImpl @Inject constructor(
    val project: Project
) : AndroidXComposeMultiplatformExtension() {
    private val multiplatformExtension =
        project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    override fun android(): Unit = multiplatformExtension.run {
        android()

        val androidMain = sourceSets.getByName("androidMain")
        val jvmMain = getOrCreateJvmMain()
        androidMain.dependsOn(jvmMain)

        val androidTest = sourceSets.getByName("androidTest")
        val jvmTest = getOrCreateJvmTest()
        androidTest.dependsOn(jvmTest)
    }

    override fun desktop(): Unit = multiplatformExtension.run {
        jvm("desktop")

        val desktopMain = sourceSets.getByName("desktopMain")
        val jvmMain = getOrCreateJvmMain()
        desktopMain.dependsOn(jvmMain)

        val desktopTest = sourceSets.getByName("desktopTest")
        val jvmTest = getOrCreateJvmTest()
        desktopTest.dependsOn(jvmTest)
    }

    override fun js(): Unit = multiplatformExtension.run {
        js(KotlinJsCompilerType.IR) {
            browser()
        }

        val commonMain = sourceSets.getByName("commonMain")
        val jsMain = sourceSets.getByName("jsMain")
        jsMain.dependsOn(commonMain)
    }

    override fun darwin(): Unit = multiplatformExtension.run {
        macosX64()
        macosArm64()
        iosX64("uikitX64")
        iosArm64("uikitArm64")

        val commonMain = sourceSets.getByName("commonMain")
        val nativeMain = sourceSets.create("nativeMain")
        val darwinMain = sourceSets.create("darwinMain")
        val macosMain = sourceSets.create("macosMain")
        val macosX64Main = sourceSets.getByName("macosX64Main")
        val macosArm64Main = sourceSets.getByName("macosArm64Main")
        val uikitMain = sourceSets.create("uikitMain")
        val uikitX64Main = sourceSets.getByName("uikitX64Main")
        val uikitArm64Main = sourceSets.getByName("uikitArm64Main")
        nativeMain.dependsOn(commonMain)
        darwinMain.dependsOn(nativeMain)
        macosMain.dependsOn(darwinMain)
        macosX64Main.dependsOn(macosMain)
        macosArm64Main.dependsOn(macosMain)
        uikitMain.dependsOn(darwinMain)
        uikitX64Main.dependsOn(uikitMain)
        uikitArm64Main.dependsOn(uikitMain)

        val commonTest = sourceSets.getByName("commonTest")
        val nativeTest = sourceSets.create("nativeTest")
        val darwinTest = sourceSets.create("darwinTest")
        val macosTest = sourceSets.create("macosTest")
        val macosX64Test = sourceSets.getByName("macosX64Test")
        val macosArm64Test = sourceSets.getByName("macosArm64Test")
        val uikitTest = sourceSets.create("uikitTest")
        val uikitX64Test = sourceSets.getByName("uikitX64Test")
        val uikitArm64Test = sourceSets.getByName("uikitArm64Test")
        nativeTest.dependsOn(commonTest)
        darwinTest.dependsOn(nativeTest)
        macosTest.dependsOn(darwinTest)
        macosX64Test.dependsOn(macosTest)
        macosArm64Test.dependsOn(macosTest)
        uikitTest.dependsOn(darwinTest)
        uikitX64Test.dependsOn(uikitTest)
        uikitArm64Test.dependsOn(uikitTest)
    }

    override fun includeUtil(): Unit {
        addUtilDirectory("commonMain", "jvmMain", "jsMain", "nativeMain")
    }

    private fun getOrCreateJvmMain(): KotlinSourceSet =
        getOrCreateSourceSet("jvmMain", "commonMain")

    private fun getOrCreateJvmTest(): KotlinSourceSet =
        getOrCreateSourceSet("jvmTest", "commonTest")

    private fun getOrCreateSourceSet(
        name: String,
        dependsOnSourceSetName: String
    ): KotlinSourceSet = multiplatformExtension.run {
        sourceSets.findByName(name)
            ?: sourceSets.create(name).apply {
                    dependsOn(sourceSets.getByName(dependsOnSourceSetName))
            }
    }

    private fun addUtilDirectory(vararg sourceSetNames: String) = multiplatformExtension.run {
        sourceSetNames.forEach { name ->
            val sourceSet = sourceSets.findByName(name)
            sourceSet?.let {
                it.kotlin.srcDirs(project.rootProject.files("compose/util/util/src/$name/kotlin/"))
            }
        }
    }
}

fun Project.experimentalOELPublication() : Boolean = findProperty("oel.publication") == "true"
fun Project.oelAndroidxVersion() : String? = findProperty("oel.androidx.version") as String?
fun Project.oelAndroidxMaterial3Version() : String? = findProperty("oel.androidx.material3.version") as String?

fun enableOELPublishing(project: Project) {
    if (!project.experimentalOELPublication()) return

    if (project.experimentalOELPublication() && (project.oelAndroidxVersion() == null)) {
        error("androidx version should be specified for OEL publications")
    }

    val ext = project.multiplatformExtension ?: error("expected a multiplatform project")

    ext.targets.all { target ->
        if (target is KotlinAndroidTarget) {
            project.publishAndroidxReference(target)
        }
    }
}

// FIXME: reflection access! Some API in Kotlin is needed
@Suppress("unchecked_cast")
private val KotlinTarget.kotlinComponents: Iterable<KotlinTargetComponent>
    get() = javaClass.kotlin.memberProperties
        .single { it.name == "kotlinComponents" }
        .get(this) as Iterable<KotlinTargetComponent>


@Suppress("unchecked_cast")
private fun Project.publishAndroidxReference(target: KotlinTarget) {
    afterEvaluate {
        target.kotlinComponents.forEach { component ->
            val componentName = component.name

            val multiplatformExtension =
                extensions.findByType(KotlinMultiplatformExtension::class.java)
                    ?: error("Expected a multiplatform project")

            if (component is KotlinVariant)
                component.publishable = false

            val usages = when (component) {
                is KotlinVariant -> component.usages
                is JointAndroidKotlinTargetComponent -> component.usages
                else -> emptyList()
            }

            extensions.getByType(PublishingExtension::class.java)
                .publications.withType(DefaultMavenPublication::class.java)
                // isAlias is needed for Gradle to ignore the fact that there's a
                // publication that is not referenced as an available-at variant of the root module
                // and has the Maven coordinates that are different from those of the root module
                // FIXME: internal Gradle API! We would rather not create the publications,
                //        but some API for that is needed in the Kotlin Gradle plugin
                .all { publication ->
                    if (publication.name == componentName) {
                        publication.isAlias = true
                    }
                }

            usages.forEach {    usage ->
                val configurationName = usage.name + "-published"

                configurations.matching{it.name == configurationName}.all() { conf ->
                    conf.artifacts.clear()
                    conf.dependencies.clear()
                    conf.setExtendsFrom(emptyList())
                    val composeVersion = requireNotNull(target.project.oelAndroidxVersion()) {
                        "Please specify oel.androidx.version property"
                    }
                    val material3Version = requireNotNull(target.project.oelAndroidxMaterial3Version()) {
                        "Please specify oel.androidx.material3.version property"
                    }

                    val version = if (target.project.group.toString().contains("org.jetbrains.compose.material3")) material3Version else composeVersion
                    val newDependency = target.project.group.toString().replace("org.jetbrains.compose", "androidx.compose") + ":" + name + ":" + version
                    conf.dependencies.add(target.project.dependencies.create(newDependency))
                }

                val rootComponent : KotlinSoftwareComponent = target.project.components.withType(KotlinSoftwareComponent::class.java)
                    .getByName("kotlin")

                (rootComponent.usages as MutableSet).add(
                    DefaultKotlinUsageContext(
                        multiplatformExtension.metadata().compilations.getByName("main"),
                        objects.named(Usage::class.java, "kotlin-api"),
                        configurationName
                    )
                )

            }
        }
    }
}

