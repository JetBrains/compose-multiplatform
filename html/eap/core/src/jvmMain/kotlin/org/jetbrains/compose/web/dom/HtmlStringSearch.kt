/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

internal actual fun isUnescapedHtmlString(value: String, attribute: Boolean): Boolean =
    value.indexOf('&') < 0 && value.indexOf('<') < 0 && value.indexOf('>') < 0 &&
        value.indexOf('\r') < 0 && value.indexOf('\u0000') < 0 &&
        (!attribute || (value.indexOf('"') < 0 && value.indexOf('\n') < 0 && value.indexOf('\t') < 0))
