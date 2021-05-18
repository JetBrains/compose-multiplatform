/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.web.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import org.w3c.dom.HTMLElement

interface DOMScope<out THTMLElement : HTMLElement>

interface ElementScope<out THTMLElement : HTMLElement> : DOMScope<THTMLElement> {

    @Composable
    @NonRestartableComposable
    fun DisposableRefEffect(
        key: Any?,
        effect: DisposableEffectScope.(THTMLElement) -> DisposableEffectResult
    )

    @Composable
    @NonRestartableComposable
    fun DisposableRefEffect(
        effect: DisposableEffectScope.(THTMLElement) -> DisposableEffectResult
    ) {
        DisposableRefEffect(null, effect)
    }

    @Composable
    @NonRestartableComposable
    fun DomSideEffect(key: Any?, effect: DomEffectScope.(THTMLElement) -> Unit)

    @Composable
    @NonRestartableComposable
    fun DomSideEffect(effect: DomEffectScope.(THTMLElement) -> Unit)
}

abstract class ElementScopeBase<out THTMLElement : HTMLElement> : ElementScope<THTMLElement> {
    abstract val element: THTMLElement

    private var nextDisposableDomEffectKey = 0

    @Composable
    @NonRestartableComposable
    override fun DisposableRefEffect(
        key: Any?,
        effect: DisposableEffectScope.(THTMLElement) -> DisposableEffectResult
    ) {
        DisposableEffect(key) { effect(element) }
    }

    @Composable
    @NonRestartableComposable
    @OptIn(ComposeCompilerApi::class)
    override fun DomSideEffect(
        key: Any?,
        effect: DomEffectScope.(THTMLElement) -> Unit
    ) {
        val changed = currentComposer.changed(key)
        val effectHolder = remember(key) {
            DomDisposableEffectHolder(this)
        }
        SideEffect {
            if (changed) effectHolder.effect(element)
        }
    }

    @Composable
    @NonRestartableComposable
    override fun DomSideEffect(effect: DomEffectScope.(THTMLElement) -> Unit) {
        DomSideEffect(nextDisposableDomEffectKey++, effect)
    }
}

open class ElementScopeImpl<THTMLElement : HTMLElement> : ElementScopeBase<THTMLElement>() {
    public override lateinit var element: THTMLElement
}

interface DomEffectScope {
    fun onDispose(disposeEffect: (HTMLElement) -> Unit)
}

private class DomDisposableEffectHolder(
    val elementScope: ElementScopeBase<HTMLElement>
) : RememberObserver, DomEffectScope {
    var onDispose: ((HTMLElement) -> Unit)? = null

    override fun onRemembered() {}

    override fun onForgotten() {
        onDispose?.invoke(elementScope.element)
    }

    override fun onAbandoned() {}

    override fun onDispose(disposeEffect: (HTMLElement) -> Unit) {
        onDispose = disposeEffect
    }
}