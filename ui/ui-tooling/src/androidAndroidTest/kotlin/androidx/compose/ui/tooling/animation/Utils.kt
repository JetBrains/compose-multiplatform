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

package androidx.compose.ui.tooling.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.updateTransition
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.tooling.CompositionDataRecord
import androidx.compose.ui.tooling.Inspectable
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.tooling.findAll
import org.junit.Assert

object Utils {

    enum class EnumState { One, Two, Three }

    val nullableFloatConverter = TwoWayConverter<Float?, AnimationVector1D>({
        AnimationVector1D(it ?: 0f)
    }, { if (it.value == 0f) null else it.value })

    val stringConverter = TwoWayConverter<String, AnimationVector1D>(
        { AnimationVector1D(it.toFloat()) }, { it.value.toString() })

    val enumConverter = object : TwoWayConverter<EnumState, AnimationVector> {
        override val convertFromVector: (AnimationVector) -> EnumState
            get() = { EnumState.One }

        override val convertToVector: (EnumState) -> AnimationVector
            get() = { AnimationVector(1f) }
    }

    val nullableEnumConverter = object :
        TwoWayConverter<EnumState?, AnimationVector> {
        override val convertFromVector: (AnimationVector) -> EnumState?
            get() = { EnumState.One }

        override val convertToVector: (EnumState?) -> AnimationVector
            get() = { AnimationVector(1f) }
    }

    val booleanConverter = object : TwoWayConverter<Boolean, AnimationVector1D> {
        override val convertFromVector: (AnimationVector1D) -> Boolean
            get() = { it.value == 1f }

        override val convertToVector: (Boolean) -> AnimationVector1D
            get() = { AnimationVector(if (it) 1f else 0f) }
    }

    @OptIn(UiToolingDataApi::class)
    internal fun ComposeContentTestRule.searchForAnimation(
        search: AnimationSearch.Search<*>,
        additionalSearch: AnimationSearch.Search<*>? = null,
        content: @Composable () -> Unit
    ) {
        val slotTableRecord = CompositionDataRecord.create()
        this.setContent {
            Inspectable(slotTableRecord) {
                content()
            }
        }
        this.runOnUiThread {
            val groups = slotTableRecord.store.map { it.asTree() }
                .flatMap { tree -> tree.findAll { it.location != null } }
            search.addAnimations(groups)
            additionalSearch?.addAnimations(groups)
        }
    }

    @OptIn(UiToolingDataApi::class)
    internal fun ComposeContentTestRule.searchAndTrackAllAnimations(
        search: AnimationSearch,
        content: @Composable () -> Unit
    ) {
        val slotTableRecord = CompositionDataRecord.create()
        this.setContent {
            Inspectable(slotTableRecord) {
                content()
            }
        }
        this.runOnUiThread {
            val groups = slotTableRecord.store.map { it.asTree() }
                .flatMap { tree -> tree.findAll { it.location != null } }
            search.findAll(groups)
            search.trackAll()
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun createTestAnimatedVisibility(): Transition<Boolean> {
        val selected by remember { mutableStateOf(false) }
        val transition = updateTransition(selected, "TestAnimatedVisibility")
        transition.AnimatedVisibility(
            visible = { it },
        ) {
            Text(text = "It is fine today.")
        }
        return transition
    }

    fun assertEquals(expected: Long, actual: Long, delta: Long) {
        Assert.assertEquals(null, expected.toFloat(), actual.toFloat(), delta.toFloat())
    }
}