/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.Project

// Plugin-specific properties (also see gradle-plugins/build.gradle.kts)
open class GradlePluginConfigExtension {
    lateinit var pluginId: String
    lateinit var implementationClass: String
    var pluginPortalTags: Collection<String> = emptyList()
}

val Project.gradlePluginConfig: GradlePluginConfigExtension?
    get() = extensions.findByType(GradlePluginConfigExtension::class.java)

fun Project.gradlePluginConfig(fn: GradlePluginConfigExtension.() -> Unit) {
    extensions.create("gradlePluginConfig", GradlePluginConfigExtension::class.java).apply(fn)
}
