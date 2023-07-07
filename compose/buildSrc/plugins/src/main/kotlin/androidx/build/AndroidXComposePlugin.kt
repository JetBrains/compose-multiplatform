/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.build.Multiplatform.Companion.isJsCompilerTestsEnabled
import androidx.build.Multiplatform.Companion.isMultiplatformEnabled
import kotlin.reflect.KFunction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute

/**
 * Plugin to apply common configuration for Compose projects.
 */
class AndroidXComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val supportRoot = project.getSupportRootFolder()
        project.apply(
            mapOf<String, String>(
                "from" to "$supportRoot/buildSrc/apply/applyAndroidXComposeImplPlugin.gradle"
            )
        )
    }

    companion object {
        @JvmStatic
        fun isMultiplatformEnabled(project: Project): Boolean {
            return project.isMultiplatformEnabled()
        }

        @JvmStatic
        fun isJsCompilerTestsEnabled(project: Project): Boolean {
            return project.isJsCompilerTestsEnabled()
        }

        @JvmStatic
        fun Project.configureJsCompilerIntegrationTests() {
            if (!isMultiplatformEnabled() || !isJsCompilerTestsEnabled()) return

            val jsClasspath = configurations.create("testJsRuntimeOnly") { conf ->
                conf.isCanBeConsumed = false
                conf.isCanBeResolved = true

                conf.attributes {
                    it.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
                    it.attribute(
                        KotlinJsCompilerAttribute.jsCompilerAttribute,
                        KotlinJsCompilerAttribute.ir)
                    it.attribute(
                        Usage.USAGE_ATTRIBUTE,
                        objects.named(KotlinUsages.KOTLIN_RUNTIME)
                    )
                }
            }

            afterEvaluate {
                val dependencies by lazy {
                    jsClasspath.files { true }.joinToString(separator = ":")
                }

                tasks.withType<Test>().all { task ->
                    // force dependency on compilation for klibs built from the source
                    task.inputs.files(jsClasspath)
                    // use system property to provide files path to the runtime
                    task.systemProperty(
                        "androidx.compose.js.classpath",
                        dependencies
                    )
                }
            }
        }

        @JvmOverloads
        @JvmStatic
        fun applyAndConfigureKotlinPlugin(
            project: Project,
            isMultiplatformEnabled: Boolean = project.isMultiplatformEnabled()
        ) {
            @Suppress("UNCHECKED_CAST")
            val companion: KFunction<Unit> =
                project.extensions.getByName("applyAndConfigureKotlinPlugin") as KFunction<Unit>
            companion.call(project, isMultiplatformEnabled)
        }
    }
}
