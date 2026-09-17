/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import kotlinx.browser.dom.Document
import kotlin.js.JsName

/**
 * Keeps shared web code on the common DOM types because `kotlinx.browser.document` exposes
 * `org.w3c.dom` types during `webMain` analysis. This is public because the EAP core module
 * consumes it, while [ComposeWebInternalApi] keeps it out of the supported public API.
 */
@ComposeWebInternalApi
@JsName("document")
public external val browserDocument: Document
