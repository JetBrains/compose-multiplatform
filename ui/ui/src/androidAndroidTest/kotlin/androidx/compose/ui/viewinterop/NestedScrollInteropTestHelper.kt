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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.R
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario

internal const val MainTestList = "mainList"
internal const val OuterBoxLayout = "outerBoxLayout"
internal const val AndroidViewContainer = "androidView"

internal class NestedScrollInteropAdapter :
    RecyclerView.Adapter<NestedScrollInteropAdapter.SimpleTextViewHolder>() {
    val items = (1..200).map { it.toString() }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SimpleTextViewHolder {
        return SimpleTextViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.android_in_compose_nested_scroll_interop_list_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: SimpleTextViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SimpleTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: String) {
            itemView.findViewById<TextView>(R.id.list_item).text = item
        }
    }
}

internal open class InspectableNestedScrollConnection() : NestedScrollConnection {
    var offeredFromChild = Offset.Zero
    var velocityOfferedFromChild = Velocity.Zero
    var consumedDownChain = Offset.Zero
    var velocityConsumedDownChain = Velocity.Zero
    var notConsumedByChild = Offset.Zero
    var velocityNotConsumedByChild = Velocity.Zero

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        offeredFromChild += available
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        consumedDownChain += consumed
        notConsumedByChild += available
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        velocityOfferedFromChild += available
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        velocityConsumedDownChain += consumed
        velocityNotConsumedByChild += available
        return Velocity.Zero
    }

    fun reset() {
        offeredFromChild = Offset.Zero
        velocityOfferedFromChild = Velocity.Zero
        consumedDownChain = Offset.Zero
        velocityConsumedDownChain = Velocity.Zero
        notConsumedByChild = Offset.Zero
        velocityNotConsumedByChild = Velocity.Zero
    }
}

internal class TestNestedScrollParentView(
    context: Context,
    attrs: AttributeSet
) : CoordinatorLayout(context, attrs) {

    private val unconsumed = IntArray(2)
    val unconsumedOffset: Offset
        get() = unconsumed.toReversedOffset()

    private val offeredToParent = IntArray(2)
    val offeredToParentOffset: Offset
        get() = offeredToParent.toReversedOffset()

    private val velocityOfferedToParent = FloatArray(2)
    val velocityOfferedToParentOffset: Velocity
        get() = velocityOfferedToParent.toReversedVelocity()

    private val velocityUnconsumed = FloatArray(2)
    val velocityUnconsumedOffset: Velocity
        get() = velocityUnconsumed.toReversedVelocity()

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        super.onNestedPreScroll(target, dx, dy, consumed, type)
        offeredToParent[0] += dx
        offeredToParent[1] += dy
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        unconsumed.fill(0)
        offeredToParent.fill(0)
        return super.onStartNestedScroll(child, target, axes, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        super.onNestedScroll(
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
        unconsumed[0] += dxConsumed
        unconsumed[1] += dyConsumed
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        velocityOfferedToParent[0] += velocityX
        velocityOfferedToParent[1] += velocityY
        return super.onNestedPreFling(target, velocityX, velocityY)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        velocityUnconsumed[0] += velocityX
        velocityUnconsumed[0] += velocityY
        return super.onNestedFling(target, velocityX, velocityY, consumed)
    }
}

internal class AllConsumingInspectableConnection : InspectableNestedScrollConnection() {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        super.onPreScroll(available, source)
        return available
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        super.onPostScroll(consumed, available, source)
        return available
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        super.onPreFling(available)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        super.onPostFling(consumed, available)
        return available
    }
}

internal class RecyclerViewConsumptionTracker {
    private var consumedByRecyclerView = intArrayOf(0, 0)
    private var velocityConsumedByRecyclerView = intArrayOf(0, 0)

    val deltaConsumed
        get() = consumedByRecyclerView.toOffset()
    val velocityConsumed
        get() = velocityConsumedByRecyclerView.toComposeVelocity()

    fun trackDeltaConsumption(dx: Int, dy: Int) {
        consumedByRecyclerView[0] += dx
        consumedByRecyclerView[1] += dy
    }

    fun trackVelocityConsumed(velocityX: Int, velocityY: Int) {
        velocityConsumedByRecyclerView[0] += velocityX
        velocityConsumedByRecyclerView[1] += velocityY
    }

    fun reset() {
        consumedByRecyclerView.fill(0)
        velocityConsumedByRecyclerView.fill(0)
    }
}

@Composable
internal fun NestedScrollInteropTestApp(
    modifier: Modifier = Modifier,
    content: (Context) -> View
) {
    Box(modifier.fillMaxSize().testTag(OuterBoxLayout)) {
        AndroidView(content, modifier = Modifier.testTag(AndroidViewContainer))
    }
}

@Composable
internal fun NestedScrollDeepNested(
    modifier: Modifier,
    enabled: Boolean,
    connection: NestedScrollConnection? = null
) {
    // Box (Compose) + AndroidView (View) +
    // Box (Compose)
    val outerModifier = if (connection == null) Modifier else Modifier.nestedScroll(connection)
    NestedScrollInteropTestApp(modifier) { context ->
        LayoutInflater.from(context)
            .inflate(R.layout.test_nested_scroll_coordinator_layout_without_toolbar, null)
            .apply {
                with(findViewById<ComposeView>(R.id.compose_view)) {
                    setContent {
                        Box(modifier = outerModifier) {
                            ComposeInViewWithNestedScrollInterop()
                        }
                    }
                }
            }.also {
                ViewCompat.setNestedScrollingEnabled(it, enabled)
            }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ComposeInViewWithNestedScrollInterop() {
    LazyColumn(
        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
            .testTag(MainTestList)
    ) {
        items(200) { item ->
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(item.toString())
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
internal fun NestedScrollInteropWithView(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    recyclerViewConsumptionTracker: RecyclerViewConsumptionTracker
) {
    NestedScrollInteropTestApp(modifier) { context ->
        LayoutInflater.from(context)
            .inflate(R.layout.android_in_compose_nested_scroll_interop, null)
            .apply {
                with(findViewById<RecyclerView>(R.id.main_list)) {
                    layoutManager = LinearLayoutManager(
                        context,
                        RecyclerView.VERTICAL,
                        false
                    )
                    adapter = NestedScrollInteropAdapter()
                    setOnScrollChangeListener { _, _, _, oldX, oldY ->
                        recyclerViewConsumptionTracker.trackDeltaConsumption(oldX, oldY)
                    }
                    onFlingListener = object : RecyclerView.OnFlingListener() {
                        override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                            recyclerViewConsumptionTracker.trackVelocityConsumed(
                                velocityX,
                                velocityY
                            )
                            return false
                        }
                    }
                }
            }.also {
                ViewCompat.setNestedScrollingEnabled(it, enabled)
            }
    }
}

@ExperimentalComposeUiApi
internal fun ActivityScenario<*>.createActivityWithComposeContent(
    @LayoutRes layout: Int,
    enableInterop: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    onActivity { activity ->
        activity.setTheme(R.style.Theme_MaterialComponents_Light)
        activity.setContentView(layout)
        with(activity.findViewById<ComposeView>(R.id.compose_view)) {
            setContent {
                val nestedScrollInterop = if (enableInterop) modifier.nestedScroll(
                    rememberNestedScrollInteropConnection()
                ) else modifier
                Box(nestedScrollInterop) {
                    content()
                }
            }
        }
    }
    moveToState(Lifecycle.State.RESUMED)
}

@Composable
internal fun RecyclerViewAndroidView(interopEnabled: Boolean) {
    AndroidView(factory = { context ->
        LayoutInflater.from(context)
            .inflate(R.layout.android_in_compose_nested_scroll_interop, null)
            .apply {
                with(findViewById<RecyclerView>(R.id.main_list)) {
                    layoutManager = LinearLayoutManager(
                        context,
                        RecyclerView.VERTICAL,
                        false
                    )
                    adapter = NestedScrollInteropAdapter()
                }
            }.also {
                ViewCompat.setNestedScrollingEnabled(it, interopEnabled)
            }
    }, modifier = Modifier.testTag(AndroidViewContainer))
}

private fun IntArray.toOffset() = Offset(this[0].toFloat(), this[1].toFloat())

private fun IntArray.toComposeVelocity() =
    Velocity((this[0] * -1).toFloat(), (this[1] * -1).toFloat())

private fun IntArray.toReversedOffset(): Offset {
    require(size == 2)
    return Offset(this[0] * -1f, this[1] * -1f)
}

private fun FloatArray.toReversedVelocity(): Velocity {
    require(size == 2)
    return Velocity(this[0] * -1f, this[1] * -1f)
}

internal fun abs(velocity: Velocity) = Velocity(
    kotlin.math.abs(velocity.x),
    kotlin.math.abs(velocity.y)
)