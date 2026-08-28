/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies member defaults on every target.
package kotlinx.browser.dom.defaultarguments

import kotlinx.browser.dom.Element
import kotlinx.browser.dom.GetRootNodeOptions
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.ScrollToOptions
import kotlin.test.Test
import kotlin.test.assertNotNull

internal expect fun newDetachedNode(): Node

internal expect fun newDetachedElement(): Element

// Generated expect defaults must resolve to real defaults on each actual declaration.
class GeneratedDefaultArgumentsTest {
    @Test
    fun defaultedMemberIsCallableFromCommonCode() {
        val node = newDetachedNode()

        assertNotNull(node.cloneNode())
        assertNotNull(node.cloneNode(deep = false))
        assertNotNull(node.cloneNode(deep = true))
    }

    @Test
    fun optionDictionaryDefaultIsAvailableOnTheExpectMember() {
        val node = newDetachedNode()

        assertNotNull(node.getRootNode())
        assertNotNull(node.getRootNode(GetRootNodeOptions()))
    }

    @Test
    fun scrollDefaultsAreAvailableOnTheExpectMember() {
        val element = newDetachedElement()

        element.scroll()
        element.scroll(ScrollToOptions())
        element.scrollTo()
        element.scrollTo(ScrollToOptions())
        element.scrollBy()
        element.scrollBy(ScrollToOptions())
    }
}
