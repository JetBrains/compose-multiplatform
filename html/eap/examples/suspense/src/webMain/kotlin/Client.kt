/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)

import kotlinx.browser.dom.DocumentReadyState
import kotlinx.browser.dom.LOADING
import org.jetbrains.compose.web.hydrateRoot
import org.jetbrains.compose.web.internal.runtime.browserDocument

fun main() {
    // Wait for parsing so all stream patches and final state arrive before hydration.
    // Module scripts run after parsing, but other bundles may load earlier.
    fun hydrateUser() {
        if (browserDocument.querySelector("[data-compose-hydration-root='$UserHydrationId']") == null) return
        hydrateRoot(::deserializeUser, hydrationId = UserHydrationId) { UserView(it) }
    }
    if (browserDocument.readyState == DocumentReadyState.LOADING) {
        browserDocument.addEventListener("DOMContentLoaded", { hydrateUser() })
    } else {
        hydrateUser()
    }
}
