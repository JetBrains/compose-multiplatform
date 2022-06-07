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

package androidx.compose.runtime

import android.view.View
import android.widget.TextView
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RecomposerTests : BaseComposeTest() {

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testNativeViewWithAttributes() {
        compose {
            TextView(id = 456, text = "some text")
        }.then { activity ->
            assertEquals(1, activity.root.childCount)

            val tv = activity.findViewById(456) as TextView
            assertEquals("some text", tv.text)

            assertEquals(tv, activity.root.traversal().first { it is TextView })
        }
    }

    @Test
    fun testSlotKeyChangeCausesRecreate() {
        var i = 1
        var tv1: TextView? = null
        val trigger = Trigger()
        compose {
            trigger.subscribe()
            // this should cause the textview to get recreated on every compose
            i++

            key(i) {
                TextView(id = 456, text = "some text")
            }
        }.then { activity ->
            tv1 = activity.findViewById(456) as TextView
            trigger.recompose()
        }.then { activity ->
            assertEquals("Compose got called twice", 3, i)

            val tv2 = activity.findViewById(456) as TextView

            assertFalse(
                "The text views should be different instances",
                tv1 === tv2
            )

            assertEquals(
                "The unused child got removed from the view hierarchy",
                1,
                activity.root.childCount
            )
        }
    }

    // components for testing recompose behavior above
    sealed class ClickAction {
        object Recompose : ClickAction()
        class PerformOnView(val action: (View) -> Unit) : ClickAction()
    }

    @Composable fun RecomposeTestComponentsB(counter: Counter, listener: ClickAction, id: Int = 0) {
        counter.inc("$id")

        val scope = currentRecomposeScope

        TextView(
            id = id,
            onClickListener = {
                @Suppress("DEPRECATION")
                when (listener) {
                    is ClickAction.Recompose -> scope.invalidate()
                    is ClickAction.PerformOnView -> listener.action.invoke(it)
                }
            }
        )
    }

    @Test
    fun testFrameTransition() {
        var snapshotId: Int? = null
        compose {
            snapshotId = Snapshot.current.id
        }.then {
            assertNotSame(snapshotId, Snapshot.current.id)
        }
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun runningRecomposerFlow() = runTest(UnconfinedTestDispatcher()) {
        lateinit var recomposer: RecomposerInfo
        val recomposerJob = launch(TestMonotonicFrameClock(this)) {
            withRunningRecomposer {
                recomposer = it.asRecomposerInfo()
                suspendCancellableCoroutine<Unit> { }
            }
        }
        val afterLaunch = Recomposer.runningRecomposers.value
        assertTrue("recomposer in running list", recomposer in afterLaunch)
        recomposerJob.cancelAndJoin()
        val afterCancel = Recomposer.runningRecomposers.value
        assertFalse("recomposer no longer in running list", recomposer in afterCancel)
    }
}

class Counter {
    private var counts = mutableMapOf<String, Int>()
    fun inc(key: String) = counts.getOrPut(key, { 0 }).let { counts[key] = it + 1 }
    operator fun get(key: String) = counts[key] ?: 0
}

private class Trigger {
    val count = mutableStateOf(0)
    fun subscribe() { count.value }
    fun recompose() { count.value += 1 }
}
