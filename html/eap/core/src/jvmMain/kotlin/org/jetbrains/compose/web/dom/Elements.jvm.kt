/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

private val buildersCache = mutableMapOf<String, ElementBuilder<*>>()

internal actual val platformElementBuildersCache: Map<String, ElementBuilder<*>>
    get() = buildersCache

@Suppress("UNCHECKED_CAST")
internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> = buildersCache.getOrPut(tagName) {
    object : ElementBuilder<Element> {
        override val tagName: String = tagName

        override fun create(): Element = throw UnsupportedOperationException(
            "DOM element creation for <$tagName> is not available on JVM"
        )
    }
} as ElementBuilder<TElement>
