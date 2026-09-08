/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import org.jetbrains.compose.web.css.formatCssNumber
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

/**
 * Produces identical numeric text on all platforms for internal serialization consumers.
 *
 * Keeps `Long` values and other integers below 2^53 exact. Other values use IEEE 754 binary32
 * (`Float`) precision, so nonintegral `Double` inputs can lose digits.
 * This is the cross-module entry point for the portable number formatting implemented by CSS.
 */
@ComposeWebInternalApi
fun formatNumber(value: Number): String = formatCssNumber(value)
