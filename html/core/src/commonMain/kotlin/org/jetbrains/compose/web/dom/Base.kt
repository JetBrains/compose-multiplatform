/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import kotlinx.browser.dom.Element
import org.jetbrains.compose.web.attributes.AttrsScope

@Composable
expect fun <TElement : Element> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
)

/**
 * @param tagName - the name of the tag that needs to be created.
 * It's best to use constant values for [tagName].
 * If variable [tagName] needed, consider wrapping TagElement calls into an if...else:
 *
 * ```
 *      if (useDiv) TagElement("div", ...) else TagElement("span", ...)
 * ```
 */
@Composable
fun <TElement : Element> TagElement(
    tagName: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?,
) {
    key(tagName) {
        TagElement(
            elementBuilder = ElementBuilder.createBuilder(tagName),
            applyAttrs = applyAttrs,
            content = content,
        )
    }
}
