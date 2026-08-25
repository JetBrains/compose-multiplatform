/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Supplies JS identity checks for generated enum-like values.
package kotlinx.browser.dom.enumlike

import kotlinx.browser.JsAny

internal actual fun areIdentical(first: JsAny, second: JsAny): Boolean = first === second
