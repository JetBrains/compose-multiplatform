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
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
abstract class ComposerComposeTestCase : TestCase() {
    fun compose(composable: (ViewComposition) -> Unit) =
        ComposeTest(
            Root(composable)
        )

    class ComposeTest(val component: Component) {
        fun then(fn: (CompositionContext, Component, ViewGroup, Activity) -> Unit) {
            val controller = Robolectric.buildActivity(TestActivity::class.java)
            val activity = controller.create().get()
            val root = activity.findViewById(ROOT_ID) as ViewGroup
            val cc = Compose.createCompositionContext(root.context, root, component, null)
                cc.compose()
                fn(cc, component, root, activity)
        }
    }

    private class Root(var composable: (ViewComposition) -> Unit) : Component() {
        override fun compose() = composable(composer)
    }

    private class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(LinearLayout(this).apply { id =
                ROOT_ID
            })
        }
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

    operator fun get(key: String) = counts.getOrDefault(key, 0)
}
