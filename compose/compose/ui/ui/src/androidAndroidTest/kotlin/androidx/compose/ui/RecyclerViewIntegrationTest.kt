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

package androidx.compose.ui

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewIntegrationTest {

    private val ItemsCount = 5
    private val ScrollAmountToHideTwoItems = 220

    // array with last sizes used for drawing item of such index
    private val drawnSizes = arrayOfNulls<Float>(ItemsCount)

    // latches to be able to wait for an item to be drawn
    private val latches = Array(ItemsCount) { CountDownLatch(1) }

    @Test
    fun recycledComposeViewsAreRemeasuredAndRedrawn() {
        val activityScenario: ActivityScenario<RecyclerViewActivity> =
            ActivityScenario.launch(RecyclerViewActivity::class.java)
        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            it.setAdapter(ItemsCount) { index ->
                Box(
                    Modifier.fixedPxSize(100 + index).drawBehind {
                        drawnSizes[index] = size.height
                        latches[index].countDown()
                    }
                )
            }
        }

        assertItemIsDrawnWithCorrectSize(0)
        assertItemIsDrawnWithCorrectSize(1)

        activityScenario.onActivity {
            it.scrollBy(ScrollAmountToHideTwoItems)
        }

        assertItemIsDrawnWithCorrectSize(2)
        assertItemIsDrawnWithCorrectSize(3)

        latches[0] = CountDownLatch(1)
        latches[1] = CountDownLatch(1)
        drawnSizes[0] = null
        drawnSizes[1] = null

        activityScenario.onActivity {
            it.scrollBy(-ScrollAmountToHideTwoItems)
        }

        assertItemIsDrawnWithCorrectSize(1)
        assertItemIsDrawnWithCorrectSize(0)
    }

    private fun assertItemIsDrawnWithCorrectSize(index: Int) {
        assertWithMessage("Item with index $index wasn't drawn")
            .that(latches[index].await(3, TimeUnit.SECONDS))
            .isTrue()
        assertThat(drawnSizes[index]).isEqualTo(100f + index)
    }

    private fun Modifier.fixedPxSize(height: Int) = layout { measurable, _ ->
        val constraints = Constraints.fixed(100, height)
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

class RecyclerViewActivity : TestActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        setContentView(recyclerView, ViewGroup.LayoutParams(200, 200))
    }

    fun setAdapter(itemCount: Int, itemContent: @Composable (Int) -> Unit) {
        recyclerView.adapter = ComposeAdapter(itemCount, itemContent)
    }

    fun scrollBy(yOffset: Int) {
        recyclerView.scrollBy(0, yOffset)
    }
}

private class VH(context: Context) : RecyclerView.ViewHolder(ComposeView(context))

private class ComposeAdapter(
    val count: Int,
    val itemContent: @Composable (Int) -> Unit
) : RecyclerView.Adapter<VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent.context)

    override fun onBindViewHolder(holder: VH, position: Int) {
        (holder.itemView as ComposeView).setContent {
            itemContent(position)
        }
    }

    override fun getItemCount() = count
}
