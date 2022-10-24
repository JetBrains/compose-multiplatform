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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.DecayAnimation
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.Transition
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.firstOrNull
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

private const val UPDATE_TRANSITION = "updateTransition"
private const val ANIMATED_CONTENT = "AnimatedContent"
private const val ANIMATED_VISIBILITY = "AnimatedVisibility"
private const val ANIMATE_VALUE_AS_STATE = "animateValueAsState"
private const val REMEMBER = "remember"
private const val SIZE_ANIMATION_MODIFIER = "androidx.compose.animation.SizeAnimationModifier"

/** Find first data with type [T] within all remember calls. */
@OptIn(UiToolingDataApi::class)
private inline fun <reified T> Collection<Group>.findRememberedData(): List<T> {
    val selfData = mapNotNull {
        it.data.firstOrNull { data ->
            data is T
        } as? T
    }
    val rememberCalls = mapNotNull { it.firstOrNull { call -> call.name == "remember" } }
    return selfData + rememberCalls.mapNotNull {
        it.data.firstOrNull { data ->
            data is T
        } as? T
    }
}

/** Contains tree parsers for different animation types. */
@OptIn(UiToolingDataApi::class)
internal class AnimationSearch {

    /** Search for animations with type [T]. */
    open class Search<T : Any>(private val trackAnimation: (T) -> Unit) {
        val animations = mutableSetOf<T>()
        open fun addAnimations(groupsWithLocation: Collection<Group>) {}
        fun hasAnimations() = animations.isNotEmpty()
        fun track() {
            // Animations are found in reversed order in the tree,
            // reverse it back so they are tracked in the order they appear in the code.
            animations.reversed().forEach(trackAnimation)
        }
    }

    /** Search for animations with type [T]. */
    open class RememberSearch<T : Any>(
        private val clazz: KClass<T>,
        trackAnimation: (T) -> Unit
    ) : Search<T>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            animations.addAll(groupsWithLocation.findRememberCallWithType(clazz).toSet())
        }

        private fun <T : Any> Collection<Group>.findRememberCallWithType(clazz: KClass<T>):
            List<T> {
            return mapNotNull {
                clazz.safeCast(
                    it.data.firstOrNull { data -> data?.javaClass?.kotlin == clazz })
            }
        }
    }

    class TargetBasedSearch(trackAnimation: (TargetBasedAnimation<*, *>) -> Unit) :
        RememberSearch<TargetBasedAnimation<*, *>>(TargetBasedAnimation::class, trackAnimation)

    class DecaySearch(trackAnimation: (DecayAnimation<*, *>) -> Unit) :
        RememberSearch<DecayAnimation<*, *>>(DecayAnimation::class, trackAnimation)

    class InfiniteTransitionSearch(trackAnimation: (InfiniteTransition) -> Unit) :
        RememberSearch<InfiniteTransition>(InfiniteTransition::class, trackAnimation)

    /** Search for animateXAsState() and animateValueAsState() animations. */
    class AnimateXAsStateSearch(trackAnimation: (Animatable<*, *>) -> Unit) :
        Search<Animatable<*, *>>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            // How "animateXAsState" calls organized:
            // Group with name "animateXAsState", for example animateDpAsState, animateIntAsState
            //    children
            //    * Group with name "animateValueAsState"
            //          children
            //          * Group with name "remember" and data with type Animatable
            //
            // To distinguish Animatable within "animateXAsState" calls from other Animatables,
            // first "animateValueAsState" calls are found.
            //  Find Animatable within "animateValueAsState" call.
            animations.addAll(
                groupsWithLocation.filter { call -> call.name == ANIMATE_VALUE_AS_STATE }
                    .mapNotNull { animateValue ->
                        animateValue.children.findRememberedData<Animatable<*, *>>().firstOrNull()
                    }.toSet()
            )
        }
    }

    /** Search for animateContentSize() animations. */
    class AnimateContentSizeSearch(trackAnimation: (Any) -> Unit) :
        Search<Any>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            animations.addAll(groupsWithLocation.filter { call -> call.name == REMEMBER }
                .mapNotNull {
                    // SizeAnimationModifier is currently private.
                    it.data.firstOrNull { data ->
                        data?.javaClass?.name == SIZE_ANIMATION_MODIFIER
                    }
                }.toSet())
        }
    }

    /** Search for updateTransition() animations. */
    class TransitionSearch(trackAnimation: (Transition<*>) -> Unit) :
        Search<Transition<*>>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            // Find `updateTransition` calls.
            animations.addAll(groupsWithLocation.filter {
                it.name == UPDATE_TRANSITION
            }.findRememberedData())
        }
    }

    /** Search for AnimatedVisibility animations. */
    class AnimatedVisibilitySearch(trackAnimation: (Transition<*>) -> Unit) :
        Search<Transition<*>>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            // Find `AnimatedVisibility` calls.
            // Then, find the underlying `updateTransition` it uses.
            animations.addAll(groupsWithLocation.filter { it.name == ANIMATED_VISIBILITY }
                .mapNotNull {
                    it.children.firstOrNull { updateTransitionCall ->
                        updateTransitionCall.name == UPDATE_TRANSITION
                    }
                }.findRememberedData())
        }
    }

    /** Search for AnimatedContent animations. */
    class AnimatedContentSearch(trackAnimation: (Transition<*>) -> Unit) :
        Search<Transition<*>>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            animations.addAll(groupsWithLocation.filter { it.name == ANIMATED_CONTENT }
                .mapNotNull {
                    it.children.firstOrNull { updateTransitionCall ->
                        updateTransitionCall.name == UPDATE_TRANSITION
                    }
                }.findRememberedData())
        }
    }
}