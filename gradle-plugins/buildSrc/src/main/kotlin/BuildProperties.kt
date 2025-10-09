/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.Project

// "Global" properties
object BuildProperties {
    const val name = "JetBrains Compose Plugin"
    const val group = "org.jetbrains.compose"
    const val website = "https://www.jetbrains.com/lp/compose/"
    const val vcs = "https://github.com/JetBrains/compose-jb"
    fun composeVersion(project: Project): String =
        System.getenv("COMPOSE_GRADLE_PLUGIN_COMPOSE_VERSION")
            ?: project.findProperty("compose.version") as String
    fun composeMaterial3Version(project: Project): String =
        project.findProperty("compose.material3.version") as String
    fun testsAndroidxCompilerVersion(project: Project): String =
        project.findProperty("compose.tests.androidx.compiler.version") as String
    fun testsAndroidxCompilerCompatibleVersion(project: Project): String =
        project.findProperty("compose.tests.androidx.compatible.kotlin.version") as String
    fun deployVersion(project: Project): String =
        System.getenv("COMPOSE_GRADLE_PLUGIN_VERSION")
            ?: project.findProperty("deploy.version") as String
}
