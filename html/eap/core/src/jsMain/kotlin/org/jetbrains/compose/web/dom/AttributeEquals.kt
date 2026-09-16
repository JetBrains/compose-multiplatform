/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

// JS strings are already native, so the ordinary expression is both the simplest and fastest exact-match path.
internal actual fun Element.matchesHtmlAttribute(name: String, expected: String?): Boolean =
    namespaceURI == HtmlNamespace && getAttribute(name) == expected
