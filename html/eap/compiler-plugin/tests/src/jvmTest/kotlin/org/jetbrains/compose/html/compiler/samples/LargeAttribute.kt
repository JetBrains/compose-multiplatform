/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler.samples

import androidx.compose.runtime.Composable
import kotlinx.browser.dom.HTMLSpanElement
import org.jetbrains.compose.web.dom.*

// Folding produces a literal larger than the JVM constant pool entry limit.
private const val AttributeHundred =
    "界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界界"
private const val AttributeThousand = AttributeHundred + AttributeHundred + AttributeHundred + AttributeHundred +
        AttributeHundred + AttributeHundred + AttributeHundred + AttributeHundred + AttributeHundred + AttributeHundred
private const val AttributeUnit = AttributeThousand + AttributeThousand + AttributeThousand

@Composable
fun LargeDirectAttribute(value: String) {
    val attribute = AttributeUnit + AttributeUnit + AttributeUnit + AttributeUnit +
            AttributeUnit + AttributeUnit + AttributeUnit + AttributeUnit
    Span(attrs = { attr("title", attribute) }) { Text(value) }
}

@Composable
fun LargeStoredContentAttribute(content: ContentBuilder<HTMLSpanElement>?) {
    val attribute = AttributeUnit + AttributeUnit + AttributeUnit + AttributeUnit +
            AttributeUnit + AttributeUnit + AttributeUnit + AttributeUnit
    Span(attrs = { attr("title", attribute) }, content = content)
}

@Composable
fun LargeMixedAttribute(value: String) {
    val attribute = AttributeUnit + AttributeUnit + AttributeUnit + AttributeUnit +
            AttributeUnit + AttributeUnit + AttributeUnit + AttributeUnit
    Span(attrs = {
        attr("title", attribute)
        id(value)
    }) { Text(value) }
}
