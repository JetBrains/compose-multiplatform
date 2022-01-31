import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

fun Project.jvmTarget(version: String) {
    plugins.withId("java") {
        tasks.withType<JavaCompile> {
            sourceCompatibility = version
            targetCompatibility = version
        }
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinJvmCompile> {
            kotlinOptions.jvmTarget = version
        }
    }
}