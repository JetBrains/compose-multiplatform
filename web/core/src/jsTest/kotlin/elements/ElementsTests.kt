/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.elements

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsBuilder
import org.jetbrains.compose.web.attributes.Tag
import org.jetbrains.compose.web.core.tests.runTest
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals


class ElementsTests {
    @Test
    fun rawCreation() = runTest {
        @Composable
        fun CustomElement(
                attrs: AttrsBuilder<Tag.Div>.() -> Unit,
                content: ContentBuilder<HTMLElement>? = null
        ) {
            TagElement<Tag.Div, HTMLElement>(
                tagName = "custom",
                applyAttrs = attrs,
                content
            )
        }

        composition {
            CustomElement({
                id("container")
            }) {
                Text("CUSTOM")
            }
        }

        assertEquals("<div><custom id=\"container\">CUSTOM</custom></div>", root.outerHTML)
    }
}