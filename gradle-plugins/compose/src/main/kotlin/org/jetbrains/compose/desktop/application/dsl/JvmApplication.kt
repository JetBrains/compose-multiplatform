/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

abstract class JvmApplication {
    abstract fun from(from: SourceSet)
    abstract fun from(from: KotlinTarget)
    abstract fun disableDefaultConfiguration()
    abstract fun dependsOn(vararg tasks: Task)
    abstract fun dependsOn(vararg tasks: String)
    abstract fun fromFiles(vararg files: Any)

    abstract var mainClass: String?
    abstract val mainJar: RegularFileProperty
    abstract var javaHome: String
    abstract val args: MutableList<String>
    abstract fun args(vararg args: String)
    abstract val jvmArgs: MutableList<String>
    abstract fun jvmArgs(vararg jvmArgs: String)
    abstract val nativeDistributions: JvmApplicationDistributions
    abstract fun nativeDistributions(fn: Action<JvmApplicationDistributions>)
    abstract val buildTypes: JvmApplicationBuildTypes
    abstract fun buildTypes(fn: Action<JvmApplicationBuildTypes>)
}

