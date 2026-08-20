/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

@JvmInline
private value class JvmDisplayStyle(
    private val value: String,
) : DisplayStyle {
    override fun toString(): String = value
}

@PublishedApi
internal actual fun createDisplayStyle(value: String): DisplayStyle = JvmDisplayStyle(value)
