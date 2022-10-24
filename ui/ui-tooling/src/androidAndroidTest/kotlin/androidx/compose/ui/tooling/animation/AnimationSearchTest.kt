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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.AnimateAsStatePreview
import androidx.compose.ui.tooling.AnimateContentSizePreview
import androidx.compose.ui.tooling.AnimatedContentExtensionPreview
import androidx.compose.ui.tooling.AnimatedContentPreview
import androidx.compose.ui.tooling.AnimatedVisibilityPreview
import androidx.compose.ui.tooling.CrossFadePreview
import androidx.compose.ui.tooling.DecayAnimationPreview
import androidx.compose.ui.tooling.InfiniteTransitionPreview
import androidx.compose.ui.tooling.TargetBasedAnimationPreview
import androidx.compose.ui.tooling.TransitionAnimatedVisibilityPreview
import androidx.compose.ui.tooling.TransitionPreview
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimationSearchTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun targetBasedAnimationIsFound() {
        var callbacks = 0
        val search = AnimationSearch.TargetBasedSearch { callbacks++ }
        rule.searchForAnimation(search) { TargetBasedAnimationPreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(200f, search.animations.first().initialValue)
    }

    @Test
    fun decayAnimationIsFound() {
        var callbacks = 0
        val search = AnimationSearch.DecaySearch { callbacks++ }
        rule.searchForAnimation(search) { DecayAnimationPreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(200f, search.animations.first().initialValue)
    }

    @Test
    fun infiniteTransitionIsFound() {
        var callbacks = 0
        val search = AnimationSearch.InfiniteTransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { InfiniteTransitionPreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
    }

    @Test
    fun animatedXAsStateSearchIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimateXAsStateSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimateAsStatePreview() }
        assertEquals(2, search.animations.size)
        search.track()
        assertEquals(2, callbacks)
        assertEquals(0.dp, search.animations.last().targetValue)
        assertEquals(2, search.animations.first().targetValue)
    }

    @Test
    fun animatedContentSizeIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimateContentSizeSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimateContentSizePreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
    }

    @Test
    fun transitionIsFound() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { TransitionPreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("checkBoxAnim", search.animations.first().label)
    }

    @Test
    fun animatedVisibilityExtensionIsFoundAsTransition() {
        var transitionSearchCallbacks = 0
        var animatedVisibilitySearchCallbacks = 0
        val transitionSearch = AnimationSearch.TransitionSearch { transitionSearchCallbacks++ }
        val animatedVisibilitySearch =
            AnimationSearch.AnimatedVisibilitySearch { animatedVisibilitySearchCallbacks++ }
        rule.searchForAnimation(transitionSearch, animatedVisibilitySearch) {
            TransitionAnimatedVisibilityPreview()
        }
        assertEquals(1, transitionSearch.animations.size)
        assertEquals(0, animatedVisibilitySearch.animations.size)
        // Track animations.
        transitionSearch.track()
        animatedVisibilitySearch.track()
        assertEquals(1, transitionSearchCallbacks)
        assertEquals(0, animatedVisibilitySearchCallbacks)
    }

    @Test
    fun crossFadeIsFoundAsTransition() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { CrossFadePreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("A", search.animations.first().targetState)
    }

    @Test
    fun animatedVisibilityIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimatedVisibilitySearch { callbacks++ }
        rule.searchForAnimation(search) { AnimatedVisibilityPreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("My Animated Visibility", search.animations.first().label)
    }

    @Test
    fun animatedContentIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimatedContentSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimatedContentPreview() }
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(0, search.animations.first().targetState)
    }

    @Test
    fun animatedContentExtensionIsFoundAsTransition() {
        var transitionCallbacks = 0
        var animatedContentCallbacks = 0
        val transitionSearch = AnimationSearch.TransitionSearch { transitionCallbacks++ }
        val animatedContentSearch =
            AnimationSearch.AnimatedContentSearch { animatedContentCallbacks++ }
        rule.searchForAnimation(transitionSearch, animatedContentSearch) {
            AnimatedContentExtensionPreview()
        }
        assertEquals(1, transitionSearch.animations.size)
        assertEquals(0, animatedContentSearch.animations.size)
        transitionSearch.track()
        animatedContentSearch.track()
        assertEquals(1, transitionCallbacks)
        assertEquals(0, animatedContentCallbacks)
    }
}