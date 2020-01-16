/*
 * Copyright 2020 The Android Open Source Project
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
@file:Suppress("PLUGIN_ERROR")
package androidx.compose

import android.os.Bundle
import android.widget.LinearLayout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith


class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            id = ROOT_ID
        })
    }
}
fun makeTestActivityRule() = ActivityTestRule(TestActivity::class.java)

private val ROOT_ID = 18284847

internal val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

internal fun Activity.uiThread(block: () -> Unit) {
    val latch = CountDownLatch(1)
    var throwable: Throwable? = null
    runOnUiThread(object : Runnable {
        override fun run() {
            try {
                block()
            } catch (e: Throwable) {
                throwable = e
            } finally {
                latch.countDown()
            }
        }
    })

    val completed = latch.await(5, TimeUnit.SECONDS)
    if (!completed) error("UI thread work did not complete within 5 seconds")
    throwable?.let {
        throw when (it) {
            is AssertionError -> AssertionError(it.localizedMessage, it)
            else ->
                IllegalStateException(
                    "UI thread threw an exception: ${it.localizedMessage}",
                    it
                )
        }
    }
}

internal fun Activity.disposeTestComposition() {
    uiThread {
        Compose.disposeComposition(root)
    }
}

internal fun Activity.show(block: @Composable() () -> Unit): Composition {
    var composition: Composition? = null
    uiThread {
        FrameManager.nextFrame()
        composition = Compose.composeInto(container = root, composable = block)
    }
    return composition!!
}

internal fun Activity.waitForAFrame() {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        throw Exception("Cannot be run from the main looper thread")
    }
    val latch = CountDownLatch(1)
    uiThread {
        Choreographer.postFrameCallback(object : ChoreographerFrameCallback {
            override fun doFrame(frameTimeNanos: Long) = latch.countDown()
        })
    }
    assertTrue(latch.await(1, TimeUnit.MINUTES),
        "Time-out waiting for choreographer frame")
}

abstract class BaseComposeTest {

    abstract val activityRule: ActivityTestRule<TestActivity>

    val activity get() = activityRule.activity

    fun compose(
        composable: ViewComposer.() -> Unit
    ) = ComposeTester(
        activity,
        composable
    )

    class ComposeTester(val activity: Activity, val composable: ViewComposer.() -> Unit) {
        lateinit var invalidateRoot: () -> Unit
        inner class ActiveTest(val activity: Activity, val composition: Composition) {
            fun recomposeRoot(): ActiveTest {
                activity.uiThread {
                    composition.compose()
                }
                activity.waitForAFrame()
                return this
            }
            fun then(block: ActiveTest.(activity: Activity) -> Unit): ActiveTest {
                activity.waitForAFrame()
                activity.uiThread {
                    block(activity)
                }
                return this
            }
        }

        fun then(block: ComposeTester.(activity: Activity) -> Unit): ActiveTest {
            var realComposable: () -> Unit = {}
            realComposable = {
                with(composer) {
                    startRestartGroup(0)
                    invalidateRoot = invalidate
                    composable()
                    endRestartGroup()?.updateScope { realComposable() }
                }
            }
            val composition = activity.show {
                realComposable()
            }
            activity.waitForAFrame()
            activity.uiThread {
                block(activity)
            }
            return ActiveTest(activity, composition)
        }
    }

}
