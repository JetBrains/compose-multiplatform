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
package androidx.compose.test

import android.os.Bundle
import android.widget.LinearLayout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import android.app.Activity
import android.view.ViewGroup
import androidx.compose.Choreographer
import androidx.compose.ChoreographerFrameCallback
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.FrameManager
import androidx.compose.Looper
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.setViewContent

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

internal fun Activity.show(block: @Composable() () -> Unit): Composition {
    var composition: Composition? = null
    uiThread {
        FrameManager.nextFrame()
        composition = setViewContent(block)
    }
    return composition!!
}

internal fun Activity.waitForAFrame() {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        throw Exception("Cannot be run from the main looper thread")
    }
    val latch = CountDownLatch(1)
    uiThread {
        Choreographer.postFrameCallback(object :
            ChoreographerFrameCallback {
            override fun doFrame(frameTimeNanos: Long) = latch.countDown()
        })
    }
    assertTrue(latch.await(1, TimeUnit.MINUTES), "Time-out waiting for choreographer frame")
}

abstract class BaseComposeTest {

    abstract val activityRule: ActivityTestRule<TestActivity>

    val activity get() = activityRule.activity

    fun compose(
        composable: @Composable() () -> Unit
    ) = UiTester(
        activity,
        composable
    )

    fun composeEmittables(
        composable: @Composable() () -> Unit
    ) = EmittableTester(
        activity,
        composable
    )
}

sealed class ComposeTester(val activity: Activity, val composable: @Composable() () -> Unit) {
    inner class ActiveTest(val activity: Activity, val composition: Composition) {
        fun then(block: ActiveTest.(activity: Activity) -> Unit): ActiveTest {
            activity.waitForAFrame()
            activity.uiThread {
                block(activity)
            }
            return this
        }

        fun done() {
            activity.waitForAFrame()
        }
    }

    abstract fun initialComposition(composable: @Composable() () -> Unit): Composition

    fun then(block: ComposeTester.(activity: Activity) -> Unit): ActiveTest {
        val composition = initialComposition(composable)
        activity.waitForAFrame()
        activity.uiThread {
            block(activity)
        }
        return ActiveTest(activity, composition)
    }
}

class EmittableTester(activity: Activity, composable: @Composable() () -> Unit) :
    ComposeTester(activity, composable) {
    override fun initialComposition(composable: @Composable() () -> Unit): Composition {
        var composition: Composition? = null
        activity.uiThread {
            FrameManager.nextFrame()
            composition = activity.setEmittableContent(composable)
        }
        return composition!!
    }
}

class UiTester(activity: Activity, composable: @Composable() () -> Unit) :
    ComposeTester(activity, composable) {
    override fun initialComposition(composable: @Composable() () -> Unit): Composition {
        return activity.show {
            composable()
        }
    }
}