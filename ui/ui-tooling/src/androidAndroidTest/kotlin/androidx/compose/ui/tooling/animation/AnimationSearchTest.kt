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

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.tooling.ComposeAnimationType
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.AnimateAsStatePreview
import androidx.compose.ui.tooling.AnimateAsStateWithLabelsPreview
import androidx.compose.ui.tooling.AnimateContentSizePreview
import androidx.compose.ui.tooling.AnimatedContentExtensionPreview
import androidx.compose.ui.tooling.AnimatedContentPreview
import androidx.compose.ui.tooling.AnimatedVisibilityPreview
import androidx.compose.ui.tooling.CrossFadePreview
import androidx.compose.ui.tooling.CrossFadeWithLabelPreview
import androidx.compose.ui.tooling.DecayAnimationPreview
import androidx.compose.ui.tooling.InfiniteTransitionPreview
import androidx.compose.ui.tooling.TargetBasedAnimationPreview
import androidx.compose.ui.tooling.TransitionAnimatedVisibilityPreview
import androidx.compose.ui.tooling.TransitionPreview
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.Utils.searchAndTrackAllAnimations
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(200f, search.animations.first().initialValue)
    }

    @Test
    fun targetBasedAnimationIsFoundButNotSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { TargetBasedAnimationPreview() }
        assertFalse(search.hasAnimations)
    }

    @Test
    fun decayAnimationIsFound() {
        var callbacks = 0
        val search = AnimationSearch.DecaySearch { callbacks++ }
        rule.searchForAnimation(search) { DecayAnimationPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(200f, search.animations.first().initialValue)
    }

    @Test
    fun decayAnimationIsFoundButNotSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { DecayAnimationPreview() }
        assertFalse(search.hasAnimations)
    }

    @Test
    fun infiniteTransitionIsFound() {
        var callbacks = 0
        val search = AnimationSearch.InfiniteTransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { InfiniteTransitionPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        val composeAnimation = search.animations.first().parse()!!
        Assert.assertNotNull(composeAnimation)
        Assert.assertNotNull(composeAnimation.animationObject)
        Assert.assertNotNull(composeAnimation.label)
        assertEquals(1, composeAnimation.states.size)
        assertEquals(ComposeAnimationType.INFINITE_TRANSITION, composeAnimation.type)
    }

    @Test
    fun infiniteTransitionIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { InfiniteTransitionPreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun multipleInfiniteTransitionIsFound() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            rememberInfiniteTransition()
            rememberInfiniteTransition()
            rememberInfiniteTransition()
            rememberInfiniteTransition()
            rememberInfiniteTransition()
        }
        assertEquals(5, search.animations.size)
        assertTrue(search.hasAnimations())
    }

    @Test
    fun animatedXAsStateSearchIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimateXAsStateSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimateAsStatePreview() }
        assertTrue(search.hasAnimations())
        assertEquals(2, search.animations.size)
        search.animations.first().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("IntAnimation", it.animatable.label)
        }
        search.animations.last().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("DpAnimation", it.animatable.label)
        }
        search.track()
        assertEquals(2, callbacks)
        assertEquals(0.dp, search.animations.last().animatable.targetValue)
        assertEquals(2, search.animations.first().animatable.targetValue)
    }

    @Test
    fun animatedXAsStateSearchIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { AnimateAsStatePreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun animatedXAsStateWithLabelsSearchIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimateXAsStateSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimateAsStateWithLabelsPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(2, search.animations.size)
        search.animations.first().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("CustomIntLabel", it.animatable.label)
        }
        search.animations.last().let {
            assertTrue(it.animationSpec is SpringSpec)
            Assert.assertNotNull(it.toolingState)
            Assert.assertNotNull(it.animatable)
            assertEquals("CustomDpLabel", it.animatable.label)
        }
        search.track()
        assertEquals(2, callbacks)
        assertEquals(0.dp, search.animations.last().animatable.targetValue)
        assertEquals(2, search.animations.first().animatable.targetValue)
    }

    @Test
    fun animatedXAsStateWithLabelsSearchIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { AnimateAsStateWithLabelsPreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun animatedContentSizeIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimateContentSizeSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimateContentSizePreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
    }

    @Test
    fun animatedContentSizeIsFoundButNotSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { AnimateContentSizePreview() }
        assertFalse(search.hasAnimations)
    }

    @Test
    fun transitionIsFound() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { TransitionPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("checkBoxAnim", search.animations.first().label)
    }

    @Test
    fun transitionIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { TransitionPreview() }
        assertTrue(search.hasAnimations)
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
        assertTrue(transitionSearch.hasAnimations())
        assertFalse(animatedVisibilitySearch.hasAnimations())
        assertEquals(1, transitionSearch.animations.size)
        assertEquals(0, animatedVisibilitySearch.animations.size)
        // Track animations.
        transitionSearch.track()
        animatedVisibilitySearch.track()
        assertEquals(1, transitionSearchCallbacks)
        assertEquals(0, animatedVisibilitySearchCallbacks)
    }

    @Test
    fun animatedVisibilityExtensionIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { TransitionAnimatedVisibilityPreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun crossFadeIsFoundAsTransition() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { CrossFadePreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("A", search.animations.first().targetState)
        assertEquals("Crossfade", search.animations.first().label)
    }

    @Test
    fun crossFadeIsFoundAsTransitionAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { CrossFadePreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun crossFadeWithLabelIsFoundAsTransition() {
        var callbacks = 0
        val search = AnimationSearch.TransitionSearch { callbacks++ }
        rule.searchForAnimation(search) { CrossFadeWithLabelPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("A", search.animations.first().targetState)
        assertEquals("CrossfadeWithLabel", search.animations.first().label)
    }

    @Test
    fun crossFadeWithLabelIsFoundAsTransitionAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { CrossFadeWithLabelPreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun animatedVisibilityIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimatedVisibilitySearch { callbacks++ }
        rule.searchForAnimation(search) { AnimatedVisibilityPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals("My Animated Visibility", search.animations.first().label)
    }

    @Test
    fun animatedVisibilityIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { AnimatedVisibilityPreview() }
        assertTrue(search.hasAnimations)
    }

    @Test
    fun animatedContentIsFound() {
        var callbacks = 0
        val search = AnimationSearch.AnimatedContentSearch { callbacks++ }
        rule.searchForAnimation(search) { AnimatedContentPreview() }
        assertTrue(search.hasAnimations())
        assertEquals(1, search.animations.size)
        search.track()
        assertEquals(1, callbacks)
        assertEquals(0, search.animations.first().targetState)
    }

    @Test
    fun animatedContentIsFoundAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { AnimatedContentPreview() }
        assertTrue(search.hasAnimations)
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
        assertTrue(transitionSearch.hasAnimations())
        assertFalse(animatedContentSearch.hasAnimations())
        assertEquals(1, transitionSearch.animations.size)
        assertEquals(0, animatedContentSearch.animations.size)
        transitionSearch.track()
        animatedContentSearch.track()
        assertEquals(1, transitionCallbacks)
        assertEquals(0, animatedContentCallbacks)
    }

    @Test
    fun animatedContentExtensionIsFoundAsTransitionAndSupported() {
        val search = AnimationSearch({ PreviewAnimationClock {} }) { }
        rule.searchAndTrackAllAnimations(search) { AnimatedContentExtensionPreview() }
        assertTrue(search.hasAnimations)
    }
}