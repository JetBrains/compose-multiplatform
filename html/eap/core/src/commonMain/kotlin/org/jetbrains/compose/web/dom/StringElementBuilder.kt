/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlinx.browser.dom.Element

internal class StringElementBuilder<TElement : Element>(
    tagName: String,
    val namespace: String = HtmlNamespace,
) : ElementBuilder<TElement> {
    override val tagName: String = normalizeElementTagName(tagName, namespace)

    override fun create(): TElement {
        throw UnsupportedOperationException(
            "String element builder for <$tagName> cannot create a DOM element"
        )
    }
}
