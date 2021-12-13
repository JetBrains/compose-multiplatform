/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.*

class ComposeProperties(private val myProject: Project) {
    val isOelPublication: Boolean
        get() = myProject.findProperty("oel.publication") == "true"

    val targetPlatforms: Set<ComposePlatforms>
        get() {
            val requestedPlatforms = myProject.findProperty("compose.platforms")?.toString() ?: "jvm, android"
            return ComposePlatforms.parse(requestedPlatforms)
        }
}
