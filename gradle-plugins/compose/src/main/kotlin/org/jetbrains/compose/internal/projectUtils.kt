/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal

import org.gradle.api.Project

internal fun Project.intProperty(name: String): Int {
    val value = project.property(name)
    return (value as? String)?.toIntOrNull()
        ?: error("Property '$name' is not an integer: $value")
}

internal fun Project.stringProperty(name: String): String {
    val value = project.property(name)
    return value as? String
        ?: error("Property '$name' is not a string: $value")

}
