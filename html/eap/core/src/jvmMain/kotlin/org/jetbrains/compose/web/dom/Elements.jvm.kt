/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

private val htmlBuildersCache = mutableMapOf<String, ElementBuilder<*>>()
private val namespacedBuildersCache = mutableMapOf<Pair<String, String>, ElementBuilder<*>>()

internal actual val platformElementBuildersCache: Map<String, ElementBuilder<*>>
    get() = htmlBuildersCache

@Suppress("UNCHECKED_CAST")
internal actual fun <TElement : Element> createPlatformElementBuilder(
    tagName: String
): ElementBuilder<TElement> = htmlBuildersCache.getOrPut(tagName) {
    object : ElementBuilder<Element> {
        override val tagName: String = tagName

        override fun create(): Element = throw UnsupportedOperationException(
            "DOM element creation for <$tagName> is not available on JVM"
        )
    }
} as ElementBuilder<TElement>

@Suppress("UNCHECKED_CAST")
internal actual fun <TElement : Element> createPlatformElementBuilderNS(
    tagName: String,
    namespace: String,
): ElementBuilder<TElement> = namespacedBuildersCache.getOrPut(namespace to tagName) {
    object : ElementBuilder<Element> {
        override val tagName: String = tagName

        override fun create(): Element = throw UnsupportedOperationException(
            "DOM element creation for <$tagName> in namespace \"$namespace\" is not available on JVM"
        )
    }
} as ElementBuilder<TElement>
