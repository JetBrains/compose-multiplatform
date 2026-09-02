/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

fun interface ElementBuilder<TElement : Element> {
    fun create(): TElement

    companion object {
        // It's internal only for testing purposes.
        internal val buildersCache: Map<String, ElementBuilder<*>>
            get() = platformElementBuildersCache

        fun <TElement : Element> createBuilder(
            tagName: String
        ): ElementBuilder<TElement> =
            createPlatformElementBuilder(tagName.lowercase())
    }
}

internal expect val platformElementBuildersCache: Map<String, ElementBuilder<*>>

internal expect fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement>
