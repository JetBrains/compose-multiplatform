/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.css

import org.jetbrains.compose.web.internal.unsafeCast

internal actual external interface TestCSSFilterValue : StylePropertyValue

internal actual fun TestCSSFilterValue(value: String): TestCSSFilterValue =
    value.unsafeCast<TestCSSFilterValue>()
