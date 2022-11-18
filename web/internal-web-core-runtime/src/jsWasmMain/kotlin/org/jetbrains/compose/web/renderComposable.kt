/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher
import org.w3c.dom.Element
import org.w3c.dom.HTMLBodyElement
import org.w3c.dom.get

/**
 * Use this method to mount the composition at the certain [root]
 *
 * @param root - the [Element] that will be the root of the DOM tree managed by Compose
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> renderComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()

    val context = monotonicFrameClock + JsMicrotasksDispatcher()
    val recomposer = Recomposer(context)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = ControlledComposition(
        applier = DomApplier(DomNodeWrapper(root)),
        parent = recomposer
    )
    val scope = object : DOMScope<TElement> {
        override val DisposableEffectScope.scopeElement: TElement
            get() = root
    }
    composition.setContent @Composable {
        content(scope)
    }
    return composition
}

/**
 * Use this method to mount the composition at the element with id - [rootElementId].
 *
 * @param rootElementId - the id of the [Element] that will be the root of the DOM tree managed
 * by Compose
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
@Suppress("UNCHECKED_CAST")
fun renderComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition = renderComposable(
    root = document.getElementById(rootElementId)!!,
    content = content
)

/**
 * Use this method to mount the composition at the [HTMLBodyElement] of the current document
 *
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
fun renderComposableInBody(
    content: @Composable DOMScope<HTMLBodyElement>.() -> Unit
): Composition = renderComposable(
    root = document.getElementsByTagName("body")[0] as HTMLBodyElement,
    content = content
)
