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

@file:Suppress("PLUGIN_ERROR")
package androidx.compose.runtime

import android.app.Activity
import android.os.Looper
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class TestActivity : ComponentActivity()

@Suppress("DEPRECATION")
fun makeTestActivityRule() = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

internal val Activity.root get() = findViewById<ViewGroup>(android.R.id.content)

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

internal fun ComponentActivity.show(block: @Composable () -> Unit) {
    uiThread {
        Snapshot.sendApplyNotifications()
        setContent(content = block)
    }
}

internal fun Activity.waitForAFrame() {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        throw Exception("Cannot be run from the main thread")
    }
    val latch = CountDownLatch(1)
    uiThread {
        Choreographer.getInstance().postFrameCallback { latch.countDown() }
    }
    assertTrue(latch.await(1, TimeUnit.HOURS), "Time-out waiting for choreographer frame")
}

abstract class BaseComposeTest {

    @Suppress("DEPRECATION")
    abstract val activityRule: androidx.test.rule.ActivityTestRule<TestActivity>

    val activity get() = activityRule.activity

    fun compose(
        composable: @Composable () -> Unit
    ) = ComposeTester(
        activity,
        composable
    )

    @Composable
    @Suppress("UNUSED_PARAMETER")
    fun subCompose(block: @Composable () -> Unit) {
//        val reference = rememberCompositionContext()
//        remember {
//            Composition(
//                UiApplier(View(activity)),
//                reference
//            )
//        }.apply {
//            setContent {
//                block()
//            }
//        }
    }
}

class ComposeTester(val activity: ComponentActivity, val composable: @Composable () -> Unit) {
    inner class ActiveTest(val activity: Activity) {
        fun then(block: ActiveTest.(activity: Activity) -> Unit): ActiveTest {
            activity.waitForAFrame()
            activity.uiThread {
                block(activity)
            }
            return this
        }

        fun done() {
            activity.uiThread {
                activity.setContentView(View(activity))
            }
            activity.waitForAFrame()
        }
    }

    private fun initialComposition(composable: @Composable () -> Unit) {
        activity.show {
            CompositionLocalProvider(
                LocalContext provides activity
            ) {
                composable()
            }
        }
    }

    fun then(block: ComposeTester.(activity: Activity) -> Unit): ActiveTest {
        initialComposition(composable)
        activity.waitForAFrame()
        activity.uiThread {
            block(activity)
        }
        return ActiveTest(activity)
    }
}

fun View.traversal(): Sequence<View> = sequence {
    yield(this@traversal)
    if (this@traversal is ViewGroup) {
        for (i in 0 until childCount) {
            yieldAll(getChildAt(i).traversal())
        }
    }
}
