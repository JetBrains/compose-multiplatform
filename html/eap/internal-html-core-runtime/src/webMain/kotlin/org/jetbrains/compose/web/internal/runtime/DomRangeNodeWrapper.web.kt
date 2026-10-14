/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import kotlinx.browser.dom.Comment
import kotlinx.browser.dom.Node

/** Groups sibling DOM nodes between comment markers without adding a layout element. */
@ComposeWebInternalApi
class DomRangeNodeWrapper(
    start: Comment = browserDocument.createComment(SuspenseMarker),
    end: Comment = browserDocument.createComment("/$SuspenseMarker"),
) : DomNodeWrapper(start) {
    var start: Comment = start
        private set
    var end: Comment = end
        private set
    override val node: Node get() = start
    private val storage = browserDocument.createDocumentFragment()
    override val logicalChildren = mutableListOf<DomNodeWrapper>()
    override val childrenParent: Node get() = checkNotNull(end.parentNode)
    override val childrenEnd: Node get() = end

    init {
        if (start.parentNode == null) {
            storage.appendChild(start)
            storage.appendChild(end)
        }
    }

    override fun domNodes(): List<Node> = buildList {
        var next: Node? = start
        while (next != null) {
            add(next)
            if (next === end) break
            next = next.nextSibling
        }
    }

    override fun detach() {
        domNodes().forEach { storage.appendChild(it) }
    }

    // Keep adopted content in an inner range so swapping the fallback preserves it.
    fun wrapContentWithin(outer: DomRangeNodeWrapper) {
        val parent = checkNotNull(outer.end.parentNode)
        val innerStart = browserDocument.createComment("$SuspenseMarker-content")
        val innerEnd = browserDocument.createComment("/$SuspenseMarker-content")
        parent.insertBefore(innerStart, outer.start.nextSibling)
        parent.insertBefore(innerEnd, outer.end)
        start = innerStart
        end = innerEnd
        outer.adoptChildren(listOf(this))
    }

    fun adoptChildren(nodes: List<DomNodeWrapper>) {
        logicalChildren.clear()
        logicalChildren.addAll(nodes)
    }

    override fun clear() {
        logicalChildren.clear()
        while (start.nextSibling != null && start.nextSibling !== end) {
            start.parentNode!!.removeChild(start.nextSibling!!)
        }
    }
}
