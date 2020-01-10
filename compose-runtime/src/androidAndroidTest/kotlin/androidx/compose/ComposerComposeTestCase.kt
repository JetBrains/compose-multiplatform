/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

import android.app.Activity
import android.view.ViewGroup

abstract class ComposerComposeTestCase {

    fun compose(
        activity: Activity,
        manualRecompose: Boolean,
        composable: (ViewComposer) -> Unit
    ) =
        ComposeTest(
            Root(composable),
            manualRecompose,
            activity
        )

    class ComposeTest(
        val component: Component,
        val manualRecompose: Boolean,
        val activity: Activity
    ) {
        internal lateinit var root: ViewGroup
        internal lateinit var cc: CompositionContext
        fun then(fn: (CompositionContext, Component, ViewGroup, Activity) -> Unit): ActiveTest {
            activity.uiThread {
                root = activity.findViewById(ROOT_ID) as ViewGroup
                cc = Compose.createCompositionContext(root.context, root, component, null)
                cc.compose()
                fn(cc, component, root, activity)
            }
            return ActiveTest()
        }

        inner class ActiveTest {
            fun then(fn: (CompositionContext, Component, ViewGroup, Activity) -> Unit): ActiveTest {
                if (!manualRecompose) {
                    activity.uiThread {
                        component.recomposeCallback?.invoke(false)
                    }
                }
                activity.waitForAFrame()
                activity.uiThread {
                    fn(cc, component, root, activity)
                }
                return this
            }
        }
    }

    private class Root(var composable: (ViewComposer) -> Unit) : Component() {
        override fun compose() = composable(composer)
    }

    companion object {
        val ROOT_ID = 18284847
    }
}

class Counter {
    private var counts = mutableMapOf<String, Int>()
    fun inc(key: String) = counts.getOrPut(key, { 0 }).let { counts[key] = it + 1 }
    fun reset() {
        counts = mutableMapOf()
    }

    operator fun get(key: String) = counts[key] ?: 0
}
