/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

import android.content.Context

class EmptyCompositionContext: CompositionContext() {
    override fun startRoot() = emptyComposition()
    override fun start(sourceHash: Int) = emptyComposition()
    override fun start(sourceHash: Int, key: Any?) = emptyComposition()
    override fun startView(sourceHash: Int) = emptyComposition()
    override fun startView(sourceHash: Int, key: Any?) = emptyComposition()
    override fun setInstance(instance: Any) = emptyComposition()
    override fun useInstance(): Any? = emptyComposition()
    override fun isInserting(): Boolean = emptyComposition()
    override fun startCompose(willCompose: Boolean) = emptyComposition()
    override fun endCompose(didCompose: Boolean) = emptyComposition()
    override fun startCall(willCall: Boolean) = emptyComposition()
    override fun endCall(didCall: Boolean) = emptyComposition()
    override fun attributeChanged(value: Any?): Boolean = emptyComposition()
    override fun attributeChangedOrInserting(value: Any?): Boolean = emptyComposition()
    override fun end() = emptyComposition()
    override fun endView() = emptyComposition()
    override fun endRoot() = emptyComposition()
    override fun applyChanges() = emptyComposition()
    override fun joinKey(left: Any?, right: Any?): Any = emptyComposition()
    override var context: Context
        get() = emptyComposition()
        set(value) = emptyComposition()
    override fun recompose(component: Component) = emptyComposition()
    override fun recomposeSync(component: Component) = emptyComposition()
    override fun preserveAmbientScope(component: Component) = emptyComposition()
    override fun <T> getAmbient(key: Ambient<T>): T = emptyComposition()
    override fun <T> getAmbient(key: Ambient<T>, component: Component): T = emptyComposition()
    override fun debug() = emptyComposition()
}

private fun emptyComposition(): Nothing = error("Composition requires an active composition context")