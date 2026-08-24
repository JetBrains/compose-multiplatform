/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

@Suppress("EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES", "EXPECT_ACTUAL_IR_INCOMPATIBILITY")
internal expect interface TestCSSFilterValue : StylePropertyValue

internal expect fun TestCSSFilterValue(value: String): TestCSSFilterValue
