/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

// JVM colors reuse the existing string carrier.
@Suppress("EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES")
actual typealias CSSColorValue = StylePropertyString

internal actual fun createCSSColorValue(value: String): CSSColorValue = createStylePropertyString(value)
