/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import kotlinx.browser.dom.events.EventListener

/**
 * An event listener that retains the DOM event name used to register it.
 *
 * This contract lives in common code because Compose HTML's event attributes
 * also need it when producing a non-browser tree. Browser-specific listener
 * registration remains in the JS DOM implementation.
 */
@ComposeWebInternalApi
interface NamedEventListener : EventListener {
    val name: String
}
