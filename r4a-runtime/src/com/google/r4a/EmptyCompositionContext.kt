/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

import android.content.Context

class EmptyCompositionContext: CompositionContext() {
    override var context: Context
        get() = emptyComposition()
        set(value) = emptyComposition()
    override fun recompose(component: Component) = emptyComposition()
    override fun recomposeSync(component: Component) = emptyComposition()
    override fun scheduleRecompose() = emptyComposition()
    override fun <T> getAmbient(key: Ambient<T>): T = emptyComposition()
    override fun addPostRecomposeObserver(l: () -> Unit) = emptyComposition()
    override fun removePostRecomposeObserver(l: () -> Unit) = emptyComposition()
}

private fun emptyComposition(): Nothing = error("Composition requires an active composition context")