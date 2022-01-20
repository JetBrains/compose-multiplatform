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

open class AndroidXComposeMultiplatformExtensionImpl @Inject constructor(
    project: Project
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
}
