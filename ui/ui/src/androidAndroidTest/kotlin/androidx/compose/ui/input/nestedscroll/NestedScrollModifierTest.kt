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

package androidx.compose.ui.input.nestedscroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class NestedScrollModifierTest {
    @get:Rule
    val rule = createComposeRule()

    private val preScrollOffset = Offset(120f, 120f)
    private val scrollOffset = Offset(125f, 125f)
    private val scrollLeftOffset = Offset(32f, 32f)
    private val preFling = Velocity(120f, 120f)
    private val postFlingConsumed = Velocity(151f, 63f)
    private val postFlingLeft = Velocity(11f, 13f)

    @Test
    fun nestedScroll_twoNodes_orderTest(): Unit = runBlocking {
        var counter = 0
        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(1)
                counter++
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(3)
                counter++
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(5)
                counter++
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(7)
                counter++
                return available
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                Box(
                    Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                )
            }
        }

        assertThat(counter).isEqualTo(0)
        counter++

        childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
        assertThat(counter).isEqualTo(2)
        counter++

        childDispatcher
            .dispatchPostScroll(scrollOffset, scrollLeftOffset, NestedScrollSource.Drag)
        assertThat(counter).isEqualTo(4)
        counter++

        childDispatcher.dispatchPreFling(preFling)
        assertThat(counter).isEqualTo(6)
        counter++

        childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)
        assertThat(counter).isEqualTo(8)
        counter++
    }

    @Test
    fun nestedScroll_NNodes_orderTest_preScroll() {
        var counter = 0
        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(2)
                counter++
                return Offset.Zero
            }
        }
        val grandParentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(1)
                counter++
                return Offset.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            counter++

            childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(counter).isEqualTo(3)
            counter++
        }
    }

    @Test
    fun nestedScroll_NNodes_orderTest_scroll() {
        var counter = 0
        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(1)
                counter++
                return Offset.Zero
            }
        }
        val grandParentConnection = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(2)
                counter++
                return Offset.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            counter++

            childDispatcher
                .dispatchPostScroll(scrollOffset, scrollLeftOffset, NestedScrollSource.Drag)
            assertThat(counter).isEqualTo(3)
            counter++
        }
    }

    @Test
    fun nestedScroll_NNodes_orderTest_preFling(): Unit = runBlocking {
        var counter = 0
        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(2)
                counter++
                return Velocity.Zero
            }
        }
        val grandParentConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(1)
                counter++
                return Velocity.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        assertThat(counter).isEqualTo(0)
        counter++

        childDispatcher.dispatchPreFling(preFling)
        assertThat(counter).isEqualTo(3)
        counter++
    }

    @Test
    fun nestedScroll_NNodes_orderTest_fling(): Unit = runBlocking {
        var counter = 0
        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(1)
                counter++
                return Velocity.Zero
            }
        }
        val grandParentConnection = object : NestedScrollConnection {
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(2)
                counter++
                return Velocity.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        assertThat(counter).isEqualTo(0)
        counter++

        childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)

        assertThat(counter).isEqualTo(3)
        counter++
    }

    @Test
    fun nestedScroll_twoNodes_hierarchyDispatch(): Unit = runBlocking {
        val preScrollReturn = Offset(60f, 30f)
        val preFlingReturn = Velocity(154f, 56f)
        var currentsource = NestedScrollSource.Drag

        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(available).isEqualTo(preScrollOffset)
                assertThat(source).isEqualTo(currentsource)
                return preScrollReturn
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(consumed).isEqualTo(scrollOffset)
                assertThat(available).isEqualTo(scrollLeftOffset)
                assertThat(source).isEqualTo(currentsource)
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(available).isEqualTo(preFling)
                return preFlingReturn
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(consumed).isEqualTo(postFlingConsumed)
                assertThat(available).isEqualTo(postFlingLeft)
                return Velocity.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                Box(
                    Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                )
            }
        }

        val preRes1 = childDispatcher.dispatchPreScroll(preScrollOffset, currentsource)
        assertThat(preRes1).isEqualTo(preScrollReturn)

        childDispatcher.dispatchPostScroll(scrollOffset, scrollLeftOffset, currentsource)
        // flip to fling to test again below
        currentsource = NestedScrollSource.Fling

        val preRes2 = childDispatcher.dispatchPreScroll(preScrollOffset, currentsource)
        assertThat(preRes2).isEqualTo(preScrollReturn)

        childDispatcher.dispatchPostScroll(scrollOffset, scrollLeftOffset, currentsource)

        val preFlingRes = childDispatcher.dispatchPreFling(preFling)
        assertThat(preFlingRes).isEqualTo(preFlingReturn)
        childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)
    }

    @Test
    fun nestedScroll_deltaCalculation_preScroll() {
        val dispatchedPreScroll = Offset(10f, 10f)
        val grandParentConsumesPreScroll = Offset(2f, 2f)
        val parentConsumedPreScroll = Offset(1f, 1f)

        val childConnection = object : NestedScrollConnection {}
        val grandParentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(available).isEqualTo(dispatchedPreScroll)
                return grandParentConsumesPreScroll
            }
        }
        val parentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(available)
                    .isEqualTo(dispatchedPreScroll - grandParentConsumesPreScroll)
                return parentConsumedPreScroll
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        rule.runOnIdle {
            val preRes =
                childDispatcher.dispatchPreScroll(dispatchedPreScroll, NestedScrollSource.Drag)
            assertThat(preRes).isEqualTo(grandParentConsumesPreScroll + parentConsumedPreScroll)
        }
    }

    @Test
    fun nestedScroll_deltaCalculation_scroll() {
        val dispatchedConsumedScroll = Offset(4f, 4f)
        val dispatchedScroll = Offset(10f, 10f)
        val grandParentConsumedScroll = Offset(2f, 2f)
        val parentConsumedScroll = Offset(1f, 1f)

        val childConnection = object : NestedScrollConnection {}
        val grandParentConnection = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(consumed).isEqualTo(parentConsumedScroll + dispatchedConsumedScroll)
                assertThat(available).isEqualTo(dispatchedScroll - parentConsumedScroll)
                return grandParentConsumedScroll
            }
        }
        val parentConnection = object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(consumed).isEqualTo(dispatchedConsumedScroll)
                assertThat(available).isEqualTo(dispatchedScroll)
                return parentConsumedScroll
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        rule.runOnIdle {
            childDispatcher.dispatchPostScroll(
                dispatchedConsumedScroll,
                dispatchedScroll,
                NestedScrollSource.Drag
            )
        }
    }

    @Test
    fun nestedScroll_deltaCalculation_preFling() = runBlocking {
        val dispatchedVelocity = Velocity(10f, 10f)
        val grandParentConsumesPreFling = Velocity(2f, 2f)
        val parentConsumedPreFling = Velocity(1f, 1f)

        val childConnection = object : NestedScrollConnection {}
        val grandParentConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(available).isEqualTo(dispatchedVelocity)
                return grandParentConsumesPreFling
            }
        }
        val parentConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(available)
                    .isEqualTo(dispatchedVelocity - grandParentConsumesPreFling)
                return parentConsumedPreFling
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        val preRes = childDispatcher.dispatchPreFling(dispatchedVelocity)
        assertThat(preRes).isEqualTo(grandParentConsumesPreFling + parentConsumedPreFling)
    }

    @Test
    fun nestedScroll_deltaCalculation_fling(): Unit = runBlocking {
        val dispatchedConsumedVelocity = Velocity(4f, 4f)
        val dispatchedLeftVelocity = Velocity(10f, 10f)
        val grandParentConsumedPostFling = Velocity(2f, 2f)
        val parentConsumedPostFling = Velocity(1f, 1f)

        val childConnection = object : NestedScrollConnection {}
        val grandParentConnection = object : NestedScrollConnection {
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(consumed)
                    .isEqualTo(parentConsumedPostFling + dispatchedConsumedVelocity)
                assertThat(available)
                    .isEqualTo(dispatchedLeftVelocity - parentConsumedPostFling)
                return grandParentConsumedPostFling
            }
        }
        val parentConnection = object : NestedScrollConnection {

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(consumed).isEqualTo(dispatchedConsumedVelocity)
                assertThat(available).isEqualTo(dispatchedLeftVelocity)
                return parentConsumedPostFling
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.requiredSize(100.dp).nestedScroll(grandParentConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(parentConnection)) {
                    Box(
                        Modifier.requiredSize(100.dp).nestedScroll(childConnection, childDispatcher)
                    )
                }
            }
        }

        childDispatcher.dispatchPostFling(dispatchedConsumedVelocity, dispatchedLeftVelocity)
    }

    @Test
    fun nestedScroll_twoNodes_flatDispatch(): Unit = runBlocking {
        val preScrollReturn = Offset(60f, 30f)
        val preFlingReturn = Velocity(154f, 56f)
        var currentsource = NestedScrollSource.Drag

        val childConnection = object : NestedScrollConnection {}
        val parentConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(available).isEqualTo(preScrollOffset)
                assertThat(source).isEqualTo(currentsource)
                return preScrollReturn
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(consumed).isEqualTo(scrollOffset)
                assertThat(available).isEqualTo(scrollLeftOffset)
                assertThat(source).isEqualTo(currentsource)
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(available).isEqualTo(preFling)
                return preFlingReturn
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(consumed).isEqualTo(postFlingConsumed)
                assertThat(available).isEqualTo(postFlingLeft)
                return Velocity.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(
                Modifier
                    .requiredSize(100.dp)
                    .nestedScroll(parentConnection) // parent
                    .nestedScroll(childConnection, childDispatcher) // child
            )
        }

        val preRes1 = childDispatcher.dispatchPreScroll(preScrollOffset, currentsource)
        assertThat(preRes1).isEqualTo(preScrollReturn)

        childDispatcher.dispatchPostScroll(scrollOffset, scrollLeftOffset, currentsource)
        // flip to fling to test again below
        currentsource = NestedScrollSource.Fling

        val preRes2 = childDispatcher.dispatchPreScroll(preScrollOffset, currentsource)
        assertThat(preRes2).isEqualTo(preScrollReturn)

        childDispatcher.dispatchPostScroll(scrollOffset, scrollLeftOffset, currentsource)

        val preFlingRes = childDispatcher.dispatchPreFling(preFling)
        assertThat(preFlingRes).isEqualTo(preFlingReturn)
        childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)
    }

    @Test
    fun nestedScroll_shouldNotCalledSelfConnection(): Unit = runBlocking {
        val childConnection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertWithMessage("self connection shouldn't be called").fail()
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertWithMessage("self connection shouldn't be called").fail()
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertWithMessage("self connection shouldn't be called").fail()
                return Velocity.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertWithMessage("self connection shouldn't be called").fail()
                return Velocity.Zero
            }
        }
        val parentConnection = object : NestedScrollConnection {}
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            Box(Modifier.nestedScroll(parentConnection)) {
                Box(Modifier.nestedScroll(childConnection, childDispatcher))
            }
        }

        childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
        childDispatcher
            .dispatchPostScroll(scrollOffset, scrollLeftOffset, NestedScrollSource.Fling)

        childDispatcher.dispatchPreFling(preFling)
        childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)
    }

    @Test
    fun nestedScroll_hierarchyDispatch_rootParentRemoval() {
        testRootParentAdditionRemoval { root, child ->
            Box(Modifier.requiredSize(100.dp).then(root)) {
                Box(child)
            }
        }
    }

    @Test
    fun nestedScroll_flatDispatch_rootParentRemoval() {
        testRootParentAdditionRemoval { root, child ->
            Box(Modifier.then(root).then(child))
        }
    }

    @Test
    fun nestedScroll_flatDispatch_longChain_rootParentRemoval() {
        testRootParentAdditionRemoval { root, child ->
            // insert a few random modifiers so it's more realistic example of wrapper re-usage
            Box(
                Modifier.requiredSize(100.dp)
                    .then(root)
                    .padding(5.dp)
                    .requiredSize(50.dp)
                    .then(child)
            )
        }
    }

    @Test
    fun nestedScroll_hierarchyDispatch_middleParentRemoval() {
        testMiddleParentAdditionRemoval { rootMod, middleMod, childMod ->
            // random boxes to emulate nesting
            Box(Modifier.requiredSize(100.dp).then(rootMod)) {
                Box {
                    Box(Modifier.requiredSize(100.dp).then(middleMod)) {
                        Box {
                            Box(childMod)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun nestedScroll_flatDispatch_middleParentRemoval() {
        testMiddleParentAdditionRemoval { rootMod, middleMod, childMod ->
            Box(
                Modifier
                    .then(rootMod)
                    .then(middleMod)
                    .then(childMod)
            )
        }
    }

    @Test
    fun nestedScroll_flatDispatch_longChain_middleParentRemoval() {
        testMiddleParentAdditionRemoval { rootMod, middleMod, childMod ->
            // insert a few random modifiers so it's more realistic example of wrapper re-usage
            Box(
                Modifier
                    .requiredSize(100.dp)
                    .then(rootMod)
                    .requiredSize(90.dp)
                    .clipToBounds()
                    .then(middleMod)
                    .padding(5.dp)
                    .then(childMod)
            )
        }
    }

    @Test
    fun nestedScroll_flatDispatch_runtimeSwapChange_orderTest() = runBlocking {
        val preScrollReturn = Offset(60f, 30f)
        val preFlingReturn = Velocity(154f, 56f)
        var counter = 0

        val isConnection1Parent = mutableStateOf(true)
        val childConnection = object : NestedScrollConnection {}
        val connection1 = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return preScrollReturn
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return preFlingReturn
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return Velocity.Zero
            }
        }
        val connection2 = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return preScrollReturn
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return preFlingReturn
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return Velocity.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            val nestedScrollParents = if (isConnection1Parent.value) {
                Modifier.nestedScroll(connection1).nestedScroll(connection2)
            } else {
                Modifier.nestedScroll(connection2).nestedScroll(connection1)
            }
            Box(
                Modifier
                    .requiredSize(100.dp)
                    .then(nestedScrollParents)
                    .nestedScroll(childConnection, childDispatcher)
            )
        }

        repeat(2) {
            counter = 1

            childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(counter).isEqualTo(3)
            counter = 1

            childDispatcher.dispatchPostScroll(
                scrollOffset,
                scrollLeftOffset,
                NestedScrollSource.Drag
            )
            assertThat(counter).isEqualTo(3)
            counter = 1

            childDispatcher.dispatchPreFling(preFling)
            assertThat(counter).isEqualTo(3)
            counter = 1

            childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)
            assertThat(counter).isEqualTo(3)
            counter = 1

            isConnection1Parent.value = !isConnection1Parent.value
            rule.waitForIdle()
        }
    }

    @Test
    fun nestedScroll_rootParentRemoval_childHasProperDispatchScope() {
        val emitParent = mutableStateOf(true)
        val parentConnection = object : NestedScrollConnection {}
        val childConnection = object : NestedScrollConnection {}
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            val parent =
                if (emitParent.value) {
                    Modifier.nestedScroll(parentConnection)
                } else {
                    Modifier
                }
            Box(parent) {
                Box(Modifier.nestedScroll(childConnection, childDispatcher))
            }
        }

        rule.runOnIdle {
            assertThat(childDispatcher.coroutineScope.isActive).isTrue()
            emitParent.value = false
        }

        rule.runOnIdle {
            // still not null as we took origin
            assertThat(childDispatcher.coroutineScope.isActive).isTrue()
        }
    }

    @Test
    fun nestedScroll_hierarchyDispatch_runtimeSwapChange_orderTest() = runBlocking {
        val preScrollReturn = Offset(60f, 30f)
        val preFlingReturn = Velocity(154f, 56f)
        var counter = 0

        val isConnection1Parent = mutableStateOf(true)
        val childConnection = object : NestedScrollConnection {}
        val connection1 = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return preScrollReturn
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return preFlingReturn
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return Velocity.Zero
            }
        }
        val connection2 = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return preScrollReturn
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 2 else 1)
                counter++
                return preFlingReturn
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(counter).isEqualTo(if (isConnection1Parent.value) 1 else 2)
                counter++
                return Velocity.Zero
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            val outerBoxConnection = if (isConnection1Parent.value) connection1 else connection2
            val innerBoxConnection = if (isConnection1Parent.value) connection2 else connection1
            Box(Modifier.requiredSize(100.dp).nestedScroll(outerBoxConnection)) {
                Box(Modifier.requiredSize(100.dp).nestedScroll(innerBoxConnection)) {
                    Box(Modifier.nestedScroll(childConnection, childDispatcher))
                }
            }
        }

        repeat(2) {
            counter = 1

            childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(counter).isEqualTo(3)
            counter = 1

            childDispatcher.dispatchPostScroll(
                scrollOffset,
                scrollLeftOffset,
                NestedScrollSource.Drag
            )
            assertThat(counter).isEqualTo(3)
            counter = 1

            childDispatcher.dispatchPreFling(preFling)
            assertThat(counter).isEqualTo(3)
            counter = 1

            childDispatcher.dispatchPostFling(postFlingConsumed, postFlingLeft)
            assertThat(counter).isEqualTo(3)
            counter = 1

            isConnection1Parent.value = !isConnection1Parent.value
            rule.waitForIdle()
        }
    }

    @Test
    fun nestedScroll_movingTarget_velocityShouldRespectSign() {
        var lastVelocity = Velocity.Zero
        val MaxOffsetBound = 900f
        val ConsumedEverything = 0.0f
        rule.setContent {
            var offset by remember {
                mutableStateOf(MaxOffsetBound)
            }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    fun consumedDelta(scrollDelta: Float): Float {
                        if (offset == 0f && scrollDelta < 0f) return ConsumedEverything
                        if (offset == MaxOffsetBound && scrollDelta > 0f) return ConsumedEverything
                        val previousOffset = offset
                        offset = (scrollDelta + offset).coerceIn(0f, MaxOffsetBound)
                        return offset - previousOffset
                    }

                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return if (available.y < 0) {
                            val consumed = consumedDelta(available.y)
                            Offset(x = 0f, y = consumed)
                        } else Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return if (abs(available.y) > 0f &&
                            available.y > 0f
                        ) {
                            Offset(0f, consumedDelta(available.y))
                        } else
                            super.onPostScroll(consumed, available, source)
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        lastVelocity = available
                        return super.onPreFling(available)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = offset
                        }
                        .nestedScroll(connection = nestedScrollConnection)
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(100) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.Gray)
                        ) {
                            BasicText(text = it.toString())
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        composeViewSwipeUp()
        rule.runOnIdle {
            // swipe ups provide negative signed velocities
            assertThat(sign(lastVelocity.y)).isEqualTo(-1)
        }
        composeViewSwipeDown()
        rule.runOnIdle {
            // swipe downs provide positive signed velocities
            assertThat(sign(lastVelocity.y)).isEqualTo(1)
        }
        composeViewSwipeDown()
        rule.runOnIdle {
            // swipe downs provide positive signed velocities
            assertThat(sign(lastVelocity.y)).isEqualTo(1)
        }
    }

// helper functions

    private fun composeViewSwipeUp() {
        onView(allOf(instanceOf(AbstractComposeView::class.java)))
            .perform(
                espressoSwipe(
                    GeneralLocation.BOTTOM_CENTER,
                    GeneralLocation.CENTER
                )
            )
    }

    private fun composeViewSwipeDown() {
        onView(allOf(instanceOf(AbstractComposeView::class.java)))
            .perform(
                espressoSwipe(
                    GeneralLocation.CENTER,
                    GeneralLocation.BOTTOM_CENTER
                )
            )
    }

    private fun espressoSwipe(
        start: CoordinatesProvider,
        end: CoordinatesProvider
    ): GeneralSwipeAction {
        return GeneralSwipeAction(
            Swipe.FAST, start, end,
            Press.FINGER
        )
    }

    private fun testMiddleParentAdditionRemoval(
        content: @Composable (root: Modifier, middle: Modifier, child: Modifier) -> Unit
    ) {
        val rootParentPreConsumed = Offset(60f, 30f)
        val parentToRemovePreConsumed = Offset(21f, 44f)

        val emitNewParent = mutableStateOf(true)
        val childConnection = object : NestedScrollConnection {}
        val rootParent = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return rootParentPreConsumed
            }
        }
        val parentToRemove = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!emitNewParent.value) {
                    assertWithMessage("Shouldn't be called when not emitted").fail()
                }
                return parentToRemovePreConsumed
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            val maybeNestedScroll =
                if (emitNewParent.value) Modifier.nestedScroll(parentToRemove) else Modifier
            content.invoke(
                Modifier.nestedScroll(rootParent),
                maybeNestedScroll,
                Modifier.nestedScroll(childConnection, childDispatcher)
            )
        }

        rule.runOnIdle {
            val res =
                childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(res).isEqualTo(rootParentPreConsumed + parentToRemovePreConsumed)

            emitNewParent.value = false
        }

        rule.runOnIdle {
            val res =
                childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(res).isEqualTo(rootParentPreConsumed)

            emitNewParent.value = true
        }

        rule.runOnIdle {
            val res =
                childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(res).isEqualTo(rootParentPreConsumed + parentToRemovePreConsumed)
        }
    }

    private fun testRootParentAdditionRemoval(
        content: @Composable (root: Modifier, child: Modifier) -> Unit
    ) {
        val preScrollReturn = Offset(60f, 30f)

        val emitParentNestedScroll = mutableStateOf(true)
        val childConnection = object : NestedScrollConnection {}
        val parent = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return preScrollReturn
            }
        }
        val childDispatcher = NestedScrollDispatcher()
        rule.setContent {
            val maybeNestedScroll =
                if (emitParentNestedScroll.value) Modifier.nestedScroll(parent) else Modifier
            content.invoke(
                maybeNestedScroll,
                Modifier.nestedScroll(childConnection, childDispatcher)
            )
        }

        rule.runOnIdle {
            val res = childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(res).isEqualTo(preScrollReturn)

            emitParentNestedScroll.value = false
        }

        rule.runOnIdle {
            val res = childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(res).isEqualTo(Offset.Zero)
            emitParentNestedScroll.value = true
        }

        rule.runOnIdle {
            val res = childDispatcher.dispatchPreScroll(preScrollOffset, NestedScrollSource.Drag)
            assertThat(res).isEqualTo(preScrollReturn)
        }
    }
}