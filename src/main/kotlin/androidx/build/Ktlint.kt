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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec

private fun Project.getKtlintConfiguration(): Configuration {
    return configurations.findByName("ktlint") ?: configurations.create("ktlint") {
        val dependency = dependencies.create("com.pinterest:ktlint:0.33.0")
        it.dependencies.add(dependency)
    }
}

fun Project.configureKtlint() {
    tasks.register("ktlint", JavaExec::class.java) { task ->
        task.description = "Check Kotlin code style."
        task.group = "Verification"
        task.classpath = getKtlintConfiguration()
        task.main = "com.pinterest.ktlint.Main"
        task.args = listOf("--android", "src/**/*.kt", "!src/**/test-data/**/*.kt")
    }

    tasks.register("ktlintFormat", JavaExec::class.java) { task ->
        task.description = "Fix Kotlin code style deviations."
        task.group = "formatting"
        task.classpath = getKtlintConfiguration()
        task.main = "com.pinterest.ktlint.Main"
        task.args = listOf("--android", "-F", "src/**/*.kt", "!src/**/test-data/**/*.kt")
    }
}