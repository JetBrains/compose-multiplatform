/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import java.lang.UnsupportedOperationException

inline fun <reified T> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}

var MavenPublication.mppArtifactId: String
    get() = throw UnsupportedOperationException()
    set(value) {
        val target = this.name
        artifactId = if ("kotlinMultiplatform" in target) value else "$value-$target"
    }