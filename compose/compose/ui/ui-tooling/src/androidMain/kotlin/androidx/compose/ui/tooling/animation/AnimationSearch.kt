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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DecayAnimation
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.tooling.data.CallGroup
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.findAll
import androidx.compose.ui.tooling.firstOrNull
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

private const val UPDATE_TRANSITION = "updateTransition"
private const val ANIMATED_CONTENT = "AnimatedContent"
private const val ANIMATED_VISIBILITY = "AnimatedVisibility"
private const val ANIMATE_VALUE_AS_STATE = "animateValueAsState"
private const val REMEMBER = "remember"
private const val REMEMBER_INFINITE_TRANSITION = "rememberInfiniteTransition"
private const val REMEMBER_UPDATED_STATE = "rememberUpdatedState"
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

@OptIn(UiToolingDataApi::class)
private inline fun <reified T> Group.findData(includeGrandchildren: Boolean = false): T? {
    // Search in self data and children data
    val dataToSearch = data + children.let {
        if (includeGrandchildren) (it + it.flatMap { child -> child.children }) else it
    }.flatMap { it.data }
    return dataToSearch.firstOrNull { data ->
        data is T
    } as? T
}

/** Contains tree parsers for different animation types. */
@OptIn(UiToolingDataApi::class)
internal class AnimationSearch(
    private val clock: () -> PreviewAnimationClock,
    private val onSeek: () -> Unit
) {
    private val transitionSearch = TransitionSearch { clock().trackTransition(it) }
    private val animatedContentSearch =
        AnimatedContentSearch { clock().trackAnimatedContent(it) }
    private val animatedVisibilitySearch = AnimatedVisibilitySearch {
        clock().trackAnimatedVisibility(it, onSeek)
    }

    private fun animateXAsStateSearch() =
        if (AnimateXAsStateComposeAnimation.apiAvailable)
            setOf(AnimateXAsStateSearch { clock().trackAnimateXAsState(it) })
        else emptyList()

    private fun infiniteTransitionSearch() =
        if (InfiniteTransitionComposeAnimation.apiAvailable)
            setOf(InfiniteTransitionSearch {
                clock().trackInfiniteTransition(it)
            })
        else emptySet()

    /** All supported animations. */
    private fun supportedSearch() = setOf(
        transitionSearch,
        animatedVisibilitySearch,
    ) + animateXAsStateSearch() + infiniteTransitionSearch() +
        (if (AnimatedContentComposeAnimation.apiAvailable)
            setOf(animatedContentSearch) else emptySet())

    private fun unsupportedSearch() = if (UnsupportedComposeAnimation.apiAvailable) setOf(
        AnimateContentSizeSearch { clock().trackAnimateContentSize(it) },
        TargetBasedSearch { clock().trackTargetBasedAnimations(it) },
        DecaySearch { clock().trackDecayAnimations(it) }
    ) else emptyList()

    /** All supported animations. */
    private val supportedSearch = supportedSearch()

    /** Animations to track in PreviewAnimationClock. */
    private val setToTrack = supportedSearch + unsupportedSearch()

    /**
     * Animations to search. animatedContentSearch is included even if it's not going to be
     * tracked as it should be excluded from transitionSearch.
     */
    private val setToSearch = setToTrack + setOf(animatedContentSearch)

    /**
     * If non of supported animations are detected, unsupported animations should not be
     * available either.
     */
    val hasAnimations: Boolean
        get() = supportedSearch.any { it.hasAnimations() }

    /**
     * Finds all animations defined in the Compose tree where the root is the
     * `@Composable` being previewed.
     */
    fun findAll(slotTrees: Collection<Group>) {
        // Check all the slot tables, since some animations might not be present in the same
        // table as the one containing the `@Composable` being previewed, e.g. when they're
        // defined using sub-composition.
        slotTrees.forEach { tree ->
            val groupsWithLocation = tree.findAll { it.location != null }
            setToSearch.forEach { it.addAnimations(groupsWithLocation) }
            // Remove all AnimatedVisibility parent transitions from the transitions list,
            // otherwise we'd duplicate them in the Android Studio Animation Preview because we
            // will track them separately.
            transitionSearch.animations.removeAll(animatedVisibilitySearch.animations)
            // Remove all AnimatedContent parent transitions from the transitions list, so we can
            // ignore these animations while support is not added to Animation Preview.
            transitionSearch.animations.removeAll(animatedContentSearch.animations)
        }
    }

    /** Make the [clock] track all the animations found. */
    fun trackAll() {
        if (hasAnimations) {
            setToTrack.forEach { it.track() }
        }
    }

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

    data class InfiniteTransitionSearchInfo(
        val infiniteTransition: InfiniteTransition,
        val toolingState: ToolingState<Long>
    )

    class InfiniteTransitionSearch(trackAnimation: (InfiniteTransitionSearchInfo) -> Unit) :
        Search<InfiniteTransitionSearchInfo>(trackAnimation) {

        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            animations.addAll(findAnimations(groupsWithLocation))
        }

        private fun findAnimations(groupsWithLocation: Collection<Group>):
            List<InfiniteTransitionSearchInfo> {
            val groups =
                groupsWithLocation.filter { group -> group.name == REMEMBER_INFINITE_TRANSITION }
                    .filterIsInstance<CallGroup>()

            return groups.mapNotNull {
                val infiniteTransition = it.findData<InfiniteTransition>()
                val toolingOverride = it.findData<MutableState<State<Long>?>>(true)
                if (infiniteTransition != null && toolingOverride != null) {
                    if (toolingOverride.value == null) {
                        toolingOverride.value = ToolingState(0L)
                    }
                    InfiniteTransitionSearchInfo(
                        infiniteTransition,
                        toolingOverride.value as ToolingState<Long>
                    )
                } else null
            }
        }
    }

    data class AnimateXAsStateSearchInfo<T, V : AnimationVector>(
        val animatable: Animatable<T, V>,
        val animationSpec: AnimationSpec<T>,
        val toolingState: ToolingState<T>
    )

    /** Search for animateXAsState() and animateValueAsState() animations. */
    class AnimateXAsStateSearch(trackAnimation: (AnimateXAsStateSearchInfo<*, *>) -> Unit) :
        Search<AnimateXAsStateSearchInfo<*, *>>(trackAnimation) {
        override fun addAnimations(groupsWithLocation: Collection<Group>) {
            animations.addAll(findAnimations<Any?>(groupsWithLocation))
        }

        private fun <T> findAnimations(groupsWithLocation: Collection<Group>):
            List<AnimateXAsStateSearchInfo<T, AnimationVector>> {
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
            val groups = groupsWithLocation.filter { group -> group.name == ANIMATE_VALUE_AS_STATE }
                .filterIsInstance<CallGroup>()
            return groups.mapNotNull {
                val animatable = findAnimatable<T>(it)
                val spec = findAnimationSpec<T>(it)
                val toolingOverride =
                    it.children.findRememberedData<MutableState<State<T>?>>().firstOrNull()
                if (animatable != null && spec != null && toolingOverride != null) {
                    if (toolingOverride.value == null) {
                        toolingOverride.value = ToolingState(animatable.value)
                    }
                    AnimateXAsStateSearchInfo(
                        animatable,
                        spec,
                        toolingOverride.value as ToolingState<T>
                    )
                } else null
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> findAnimationSpec(group: CallGroup): AnimationSpec<T>? {
            val rememberStates = group.children.filter { it.name == REMEMBER_UPDATED_STATE }
            return (rememberStates + rememberStates.flatMap { it.children })
                .flatMap { it.data }
                .filterIsInstance<State<T>>().map { it.value }
                .filterIsInstance<AnimationSpec<T>>().firstOrNull()
        }

        private fun <T> findAnimatable(group: CallGroup): Animatable<T, AnimationVector>? {
            return group.children.findRememberedData<Animatable<T, AnimationVector>>()
                .firstOrNull()
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