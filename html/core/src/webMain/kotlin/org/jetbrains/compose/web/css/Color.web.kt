/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import kotlin.js.unsafeCast

actual external interface CSSColorValue : StylePropertyValue, CSSVariableValueAs<CSSColorValue>

internal actual fun createCSSColorValue(value: String): CSSColorValue = value.unsafeCast<CSSColorValue>()
