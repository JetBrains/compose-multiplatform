/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED", "NOTHING_TO_INLINE", "FunctionName")
package org.jetbrains.compose.web.css

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> jsObject(): T =
    js("({})")

inline fun <T : Any> jsObject(builder: T.() -> Unit): T =
    jsObject<T>().apply(builder)
