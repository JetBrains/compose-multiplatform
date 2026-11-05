/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.testing

import java.io.File

/**
 * Locates the JAR or classes directory containing [markerClass], without inheriting the rest of
 * its classloader's classpath. Transitive dependencies must be supplied separately.
 */
fun classpathEntryFor(markerClass: Class<*>): File {
    val location = checkNotNull(markerClass.protectionDomain?.codeSource?.location) {
        "Cannot locate the classpath entry for ${markerClass.name}. Supply its classpath entry explicitly."
    }
    check(location.protocol == "file") {
        "Unsupported classpath location for ${markerClass.name}: $location. Supply its classpath entry explicitly."
    }
    return File(location.toURI()).also {
        check(it.exists()) { "Classpath entry for ${markerClass.name} does not exist: $it" }
    }
}

/** Locates the JAR or classes directory containing the marker type [T]. */
inline fun <reified T : Any> classpathEntryFor(): File = classpathEntryFor(T::class.java)
