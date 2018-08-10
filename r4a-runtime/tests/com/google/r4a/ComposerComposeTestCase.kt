/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.google.r4a

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
abstract class ComposerComposeTestCase : TestCase() {
    fun compose(composable: (CompositionContext) -> Unit) = ComposeTest(Root(composable))

    class ComposeTest(val component: Component) {
        fun then(fn: (CompositionContext, Component, ViewGroup, Activity) -> Unit) {
            val controller = Robolectric.buildActivity(TestActivity::class.java)
            val activity = controller.create().get()
            val root = activity.findViewById(ROOT_ID) as ViewGroup
            val cc = ComposerCompositionContext.factory(root.context, root, component)
            val prev = CompositionContext.current
            CompositionContext.current = cc
            cc.recomposeSync(component)
            fn(cc, component, root, activity)
            CompositionContext.current = prev
        }
    }

    private class Root(var composable: (CompositionContext) -> Unit) : Component() {
        override fun compose() = composable(CompositionContext.current)
    }

    private class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this).apply { id = ROOT_ID })
        }
    }

    companion object {
        val ROOT_ID = 18284847
    }

}
