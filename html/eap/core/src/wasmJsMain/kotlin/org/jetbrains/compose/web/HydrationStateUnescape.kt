/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web

@JsFun("value => value.replace(/&(lt|#13|#0|amp);/g, (_, entity) => entity === 'lt' ? '<' : entity === '#13' ? '\\r' : entity === '#0' ? '\\0' : '&')")
private external fun unescapeHydrationState(value: String): String

// One native pass preserves the protocol's non-recursive ampersand-last decoding.
internal actual fun String.unescapeFromHydrationStateElement(): String =
    unescapeHydrationState(this)
