/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Used to check the size of the RecycledViewPool
 */
private const val MaxItemsInAnyTest = 100

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
/**
 * Note: this test's structure largely parallels PoolingContainerRecyclerViewTest
 * (though there are notable implementation differences)
 *
 * Consider if new tests added here should also be added there.
 */
class AndroidComposeViewsRecyclerViewTest {
    @get:Rule
    val animationRule = AnimationDurationScaleRule.create()

    @get:Rule
    var activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    lateinit var recyclerView: RecyclerView
    lateinit var container: FrameLayout

    private val instrumentation = InstrumentationRegistry.getInstrumentation()!!

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            container = FrameLayout(activity)
            container.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            activity.setContentView(container)
            recyclerView = RecyclerView(activity)
            setUpRecyclerView(recyclerView)
            container.addView(recyclerView)
        }
    }

    private fun setUpRecyclerView(rv: RecyclerView) {
        activityRule.scenario.onActivity { activity ->
            // Animators cause items to stick around and prevent clean rebinds, which we don't want,
            // since it makes testing this less straightforward.
            rv.itemAnimator = null
            rv.layoutManager =
                LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            rv.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100
            )
        }
    }

    @Test
    fun allItemsChanged_noDisposals() {
        lateinit var adapter: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100)
            recyclerView.adapter = adapter
        }
        instrumentation.runOnMainSync { }

        // All items created and bound
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.binds).isEqualTo(100)

        instrumentation.runOnMainSync { adapter.notifyItemRangeChanged(0, 100) }
        instrumentation.runOnMainSync { }

        // All items changed: no new creations, but all items rebound
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)
        assertThat(adapter.binds).isEqualTo(200)
    }

    @Test
    fun viewDiscarded_allDisposed() {
        lateinit var adapter: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100)
            recyclerView.adapter = adapter
        }

        instrumentation.runOnMainSync { }
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)

        instrumentation.runOnMainSync { container.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
    }

    @Test
    fun reattachedAndDetached_disposedTwice() {
        lateinit var adapter: PoolingContainerTestAdapter

        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100)
            recyclerView.adapter = adapter
        }
        instrumentation.runOnMainSync { }

        // Initially added: all items created, no disposals
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)

        instrumentation.runOnMainSync { container.removeAllViews() }

        // Removed: all items disposed
        assertThat(adapter.releases).isEqualTo(100)

        activityRule.scenario.onActivity { container.addView(recyclerView) }

        // Re-added: no new disposals, no new creations, all items recomposed
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(200)
        assertThat(adapter.releases).isEqualTo(100)

        activityRule.scenario.onActivity { container.removeAllViews() }

        // Removed again: all items disposed a second time
        assertThat(adapter.releases).isEqualTo(200)
    }

    @Test
    fun poolReplaced_allDisposed() = runBlocking {
        lateinit var adapter: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100, 2)
            val pool = recyclerView.recycledViewPool
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
            recyclerView.adapter = adapter
        }
        instrumentation.runOnMainSync { }
        assertThat(recyclerView.height).isEqualTo(100)
        assertThat(adapter.creations).isEqualTo(50)

        // Scroll to put some views into the shared pool
        instrumentation.runOnMainSync {
            recyclerView.smoothScrollBy(0, 100)
        }

        recyclerView.awaitScrollIdle()

        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)

        // Swap pool, confirm contents of old pool are disposed
        instrumentation.runOnMainSync {
            recyclerView.setRecycledViewPool(RecyclerView.RecycledViewPool())
        }
        activityRule.scenario.onActivity { container.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
    }

    @Test
    fun poolCleared_allDisposed() = runBlocking {
        lateinit var adapter: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100, 2)
        }
        instrumentation.runOnMainSync {
            val pool = recyclerView.recycledViewPool
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
            recyclerView.adapter = adapter
        }

        instrumentation.runOnMainSync { }

        // Scroll to put some views into the shared pool
        instrumentation.runOnMainSync {
            recyclerView.smoothScrollBy(0, 100)
        }

        recyclerView.awaitScrollIdle()

        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)

        // Clear pool, remove from Activity, confirm contents of pool are disposed
        instrumentation.runOnMainSync {
            recyclerView.recycledViewPool.clear()
            container.removeAllViews()
        }
        assertThat(adapter.releases).isEqualTo(100)
    }

    @Test
    fun setAdapter_allDisposed() {
        // Replacing the adapter when it is the only adapter attached to the pool means that
        // the pool is cleared, so everything should be disposed.
        doSetOrSwapTest(expectedDisposalsAfterBlock = 100) {
            recyclerView.adapter = it
        }
    }

    @Test
    fun swapAdapter_noDisposals() {
        doSetOrSwapTest(expectedDisposalsAfterBlock = 0) {
            recyclerView.swapAdapter(it, false)
        }
    }

    @Test
    fun setAdapterToNull_allDisposed() {
        doSetOrSwapTest(expectedDisposalsAfterBlock = 100) {
            recyclerView.adapter = null
        }
    }

    private fun doSetOrSwapTest(
        expectedDisposalsAfterBlock: Int,
        setOrSwapBlock: (PoolingContainerTestAdapter) -> Unit,
    ) = runBlocking {
        lateinit var adapter: PoolingContainerTestAdapter
        lateinit var adapter2: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100, 2)
            adapter2 = PoolingContainerTestAdapter(activity, 100, 2)
            val pool = recyclerView.recycledViewPool
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
            recyclerView.adapter = adapter
        }
        instrumentation.runOnMainSync { }

        // Scroll to put some views into the shared pool
        withContext(Dispatchers.Main) {
            recyclerView.smoothScrollBy(0, 100)
        }
        recyclerView.awaitScrollIdle()

        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.releases).isEqualTo(0)

        withContext(Dispatchers.Main) {
            // Set or swap adapter, confirm expected results
            setOrSwapBlock(adapter2)
        }

        assertThat(adapter.releases + adapter2.releases).isEqualTo(expectedDisposalsAfterBlock)

        // Remove the RecyclerView, confirm everything is disposed
        instrumentation.runOnMainSync { container.removeAllViews() }
        assertThat(adapter.releases).isEqualTo(100)
        assertThat(adapter2.creations).isEqualTo(adapter2.releases)
        assertThat(adapter2.compositions).isEqualTo(adapter2.creations)
        // ...and that nothing unexpected happened
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
    }

    @Test
    fun overflowingScrapTest() {
        lateinit var adapter: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100)
            recyclerView.adapter = adapter
            val pool = recyclerView.recycledViewPool
            for (i in 0..9) {
                // We'll generate more scrap views of each type than this
                pool.setMaxRecycledViews(i, 3)
            }
        }

        instrumentation.runOnMainSync { }

        // All items created and bound
        assertThat(adapter.creations).isEqualTo(100)
        assertThat(adapter.compositions).isEqualTo(100)
        assertThat(adapter.binds).isEqualTo(100)

        // Simulate removing and re-adding the first 100 items
        instrumentation.runOnMainSync {
            adapter.notifyItemRangeRemoved(0, 100)
            adapter.notifyItemRangeInserted(0, 100)
        }
        instrumentation.runOnMainSync { }

        assertThat(adapter.creations).isEqualTo(200)
        assertThat(adapter.compositions).isEqualTo(200)

        instrumentation.runOnMainSync { container.removeAllViews() }

        // Make sure that all views were disposed, including those that never made it to the pool
        assertThat(adapter.releases).isEqualTo(200)
    }

    @Test
    fun sharedViewPool() = runBlocking {
        val itemViewCacheSize = 2
        instrumentation.runOnMainSync {
            container.removeAllViews()
        }
        val lp1 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        val lp2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        val rv1: RecyclerView = recyclerView.also { it.layoutParams = lp1 }
        lateinit var rv2: RecyclerView
        lateinit var testContainer: LinearLayout
        val pool = RecyclerView.RecycledViewPool()
        lateinit var adapter1: PoolingContainerTestAdapter
        lateinit var adapter2: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter1 = PoolingContainerTestAdapter(activity, 100, 10)
            adapter2 = PoolingContainerTestAdapter(activity, 100, 10)

            rv2 = RecyclerView(activity).also { setUpRecyclerView(it); it.layoutParams = lp2 }
            testContainer = LinearLayout(activity).also {
                it.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    200
                )
                it.orientation = LinearLayout.VERTICAL
            }
            rv1.setItemViewCacheSize(itemViewCacheSize)
            rv2.setItemViewCacheSize(itemViewCacheSize)
            rv1.adapter = adapter1
            rv2.adapter = adapter2
            testContainer.addView(rv1)
            testContainer.addView(rv2)
            container.addView(testContainer)
            for (i in 0..9) {
                pool.setMaxRecycledViews(i, 10)
            }
            rv1.setRecycledViewPool(pool)
            rv2.setRecycledViewPool(pool)
        }

        instrumentation.runOnMainSync { }
        assertThat(adapter1.creations).isEqualTo(10)
        assertThat(adapter1.compositions).isEqualTo(10)

        // Scroll to put some views into the shared pool
        instrumentation.runOnMainSync {
            rv1.smoothScrollBy(0, 100)
        }
        rv1.awaitScrollIdle()

        // The RV keeps a couple items in its view cache before returning them to the pool
        val expectedRecycledItems = 10 - itemViewCacheSize
        assertThat(pool.getRecycledViewCount(0)).isEqualTo(expectedRecycledItems)

        // Nothing should have been disposed yet, everything should have gone to the pool
        assertThat(adapter1.releases + adapter2.releases).isEqualTo(0)

        val adapter1Creations = adapter1.creations
        // There were 10, we scrolled 10 more into view, plus maybe prefetching
        assertThat(adapter1Creations).isAtLeast(20)
        val adapter1Compositions = adapter1.compositions
        // Currently, prefetched views don't end up being composed, but that could change
        assertThat(adapter1Compositions).isAtLeast(20)

        // Remove the first RecyclerView
        instrumentation.runOnMainSync {
            testContainer.removeView(rv1)
        }
        instrumentation.runOnMainSync { } // get the relayout

        // After the first RecyclerView is removed, we expect everything it created to be disposed,
        // *except* for what's in the shared pool
        assertThat(adapter1.creations).isEqualTo(adapter1Creations) // just checking
        assertThat(adapter1Compositions).isEqualTo(adapter1.compositions) // just checking
        assertThat(pool.size).isEqualTo(expectedRecycledItems)
        // We need to check compositions, not creations, because if it's not composed, it won't be
        // disposed.
        assertThat(adapter1.releases).isEqualTo(adapter1.compositions - expectedRecycledItems)
        assertThat(adapter2.creations).isEqualTo(20) // it's twice as tall with rv1 gone
        assertThat(adapter2.compositions).isEqualTo(20) // it's twice as tall with rv1 gone
        assertThat(adapter2.releases).isEqualTo(0) // it hasn't scrolled

        instrumentation.runOnMainSync {
            testContainer.removeView(rv2)
        }
        assertThat(adapter1.creations).isEqualTo(adapter1Creations) // just to be really sure...
        // double-check that nothing weird happened
        assertThat(adapter1.compositions).isEqualTo(20)
        // at this point they're all off
        assertThat(adapter1.releases).isEqualTo(adapter1.compositions)
        assertThat(adapter2.creations).isEqualTo(20) // again, just checking
        assertThat(adapter2.compositions).isEqualTo(20) // again, just checking
        assertThat(adapter2.releases).isEqualTo(20) // all of these should be gone too
    }

    @Test
    fun animationTest() = runBlocking {
        animationRule.setAnimationDurationScale(1f)

        withContext(Dispatchers.Main) {
            recyclerView.itemAnimator = DefaultItemAnimator()
        }

        lateinit var adapter: PoolingContainerTestAdapter
        activityRule.scenario.onActivity { activity ->
            adapter = PoolingContainerTestAdapter(activity, 100, itemHeightPx = 2)
            recyclerView.adapter = adapter
        }
        awaitFrame()

        // All this needs to be on the main thread so that the animation doesn't progress and lead
        // to race conditions.
        withContext(Dispatchers.Main) {
            // Remove all onscreen items
            adapter.items = 50
            adapter.notifyItemRangeRemoved(0, 50)

            // For some reason, one frame isn't enough
            awaitFrame()
            awaitFrame()

            // Animation started: 50 new items created, existing 50 animating out
            // and so they can't be released yet
            assertThat(adapter.releases).isEqualTo(0)
            assertThat(adapter.creations).isEqualTo(100)
            assertThat(adapter.compositions).isEqualTo(100)

            // After the animation, the original 50 are either disposed or in the pool
            recyclerView.awaitItemAnimationsComplete()
            // Assumption check: if they're *all* in the pool,
            // this test isn't very useful and we need to make the pool smaller for this test.
            assertThat(adapter.releases).isGreaterThan(0)
            assertThat(adapter.releases).isEqualTo(50 - recyclerView.recycledViewPool.size)
            assertThat(adapter.creations).isEqualTo(100)
            assertThat(adapter.compositions).isEqualTo(100)
        }
    }
}

class PoolingContainerTestAdapter(
    val context: Context,
    var items: Int,
    private val itemHeightPx: Int = 1
) : RecyclerView.Adapter<PoolingContainerTestAdapter.ViewHolder>() {
    init {
        if (items > MaxItemsInAnyTest) {
            throw IllegalArgumentException(
                "$items > $MaxItemsInAnyTest, increase MaxItemsInAnyTest"
            )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var creations = 0
    var compositions = 0
    var binds = 0
    var releases = 0

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DisposalCountingComposeView(context, this)
        view.layoutParams =
            RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeightPx)

        creations++

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        binds++
    }

    override fun getItemViewType(position: Int): Int {
        return position / 10
    }

    override fun getItemCount(): Int = items
}

class DisposalCountingComposeView(
    context: Context,
    private val adapter: PoolingContainerTestAdapter
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        DisposableEffect(true) {
            adapter.compositions++
            onDispose {
                adapter.releases++
            }
        }
    }
}

private suspend fun RecyclerView.awaitScrollIdle() {
    val rv = this
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val listener = object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        continuation.resume(Unit)
                    }
                }
            }

            rv.addOnScrollListener(listener)

            continuation.invokeOnCancellation { rv.removeOnScrollListener(listener) }

            if (rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                continuation.resume(Unit)
            }
        }
    }
}

private suspend fun RecyclerView.awaitItemAnimationsComplete() {
    val rv = this
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val animator = rv.itemAnimator ?: throw IllegalStateException(
                "awaitItemAnimationsComplete() was called on a RecyclerView with no ItemAnimator." +
                    " This may have been unintended."
            )
            animator.isRunning { continuation.resume(Unit) }
        }
    }
}

private val RecyclerView.RecycledViewPool.size: Int
    get() {
        var items = 0
        for (type in 0..(MaxItemsInAnyTest - 1) / 10) {
            items += this.getRecycledViewCount(type)
        }
        return items
    }