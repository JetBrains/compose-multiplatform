/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationBuildTypes
import org.jetbrains.compose.internal.utils.new
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import javax.inject.Inject

internal open class JvmApplicationInternal @Inject constructor(
    val name: String,
    objects: ObjectFactory
) : JvmApplication() {
    internal val data: JvmApplicationData = objects.new()

    final override fun from(from: SourceSet) {
        data.jvmApplicationRuntimeFilesProvider = JvmApplicationRuntimeFilesProvider.FromGradleSourceSet(from)
    }
    final override fun from(from: KotlinTarget) {
        check(from is KotlinJvmTarget) { "Non JVM Kotlin MPP targets are not supported: ${from.javaClass.canonicalName} " +
                "is not subtype of ${KotlinJvmTarget::class.java.canonicalName}" }
        data.jvmApplicationRuntimeFilesProvider = JvmApplicationRuntimeFilesProvider.FromKotlinMppTarget(from)
    }
    final override fun disableDefaultConfiguration() {
        data.isDefaultConfigurationEnabled = false
    }

    final override fun fromFiles(vararg files: Any) {
        data.fromFiles.from(*files)
    }

    final override fun dependsOn(vararg tasks: String) {
        data.dependenciesTaskNames.addAll(tasks)
    }
    final override fun dependsOn(vararg tasks: Task) {
        tasks.mapTo(data.dependenciesTaskNames) { it.path }
    }

    final override var mainClass: String? by data::mainClass
    final override val mainJar: RegularFileProperty by data::mainJar
    final override var javaHome: String by data::javaHome

    final override val args: MutableList<String> by data::args
    final override fun args(vararg args: String) {
        data.args.addAll(args)
    }

    final override val jvmArgs: MutableList<String> by data::jvmArgs
    final override fun jvmArgs(vararg jvmArgs: String) {
        data.jvmArgs.addAll(jvmArgs)
    }

    final override val nativeDistributions: JvmApplicationDistributions by data::nativeDistributions
    final override fun nativeDistributions(fn: Action<JvmApplicationDistributions>) {
        fn.execute(data.nativeDistributions)
    }

    final override val buildTypes: JvmApplicationBuildTypes by data::buildTypes
    final override fun buildTypes(fn: Action<JvmApplicationBuildTypes>) {
        fn.execute(data.buildTypes)
    }
}