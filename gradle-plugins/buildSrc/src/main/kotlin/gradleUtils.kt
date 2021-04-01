/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.Project

inline fun <reified T> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}