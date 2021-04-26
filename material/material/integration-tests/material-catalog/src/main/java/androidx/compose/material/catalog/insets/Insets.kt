/*
 * Copyright 2021 The Android Open Source Project
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

/**
 * TODO: Move to depending on Accompanist with prebuilts when we hit a stable version
 * https://github.com/google/accompanist/blob/main/insets/src/main/java/com/google/accompanist
 * /insets/Insets.kt
 */

@file:Suppress("NOTHING_TO_INLINE", "unused", "PropertyName")

@file:JvmName("ComposeInsets")
@file:JvmMultifileClass

package androidx.compose.material.catalog.insets

import android.annotation.SuppressLint
import android.view.View
import android.view.WindowInsetsAnimation
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Main holder of our inset values.
 */
@Stable
class WindowInsets {
    /**
     * Inset values which match [WindowInsetsCompat.Type.systemBars]
     */
    val systemBars: InsetsType = InsetsType()

    /**
     * Inset values which match [WindowInsetsCompat.Type.systemGestures]
     */
    val systemGestures: InsetsType = InsetsType()

    /**
     * Inset values which match [WindowInsetsCompat.Type.navigationBars]
     */
    val navigationBars: InsetsType = InsetsType()

    /**
     * Inset values which match [WindowInsetsCompat.Type.statusBars]
     */
    val statusBars: InsetsType = InsetsType()

    /**
     * Inset values which match [WindowInsetsCompat.Type.ime]
     */
    val ime: InsetsType = InsetsType()
}

/**
 * Represents the values for a type of insets, and stores information about the layout insets,
 * animating insets, and visibility of the insets.
 *
 * [InsetsType] instances are commonly stored in a [WindowInsets] instance.
 */
@Stable
@Suppress("MemberVisibilityCanBePrivate")
class InsetsType : Insets {
    private var ongoingAnimationsCount by mutableStateOf(0)
    internal val _layoutInsets = MutableInsets()
    internal val _animatedInsets = MutableInsets()

    /**
     * The layout insets for this [InsetsType]. These are the insets which are defined from the
     * current window layout.
     *
     * You should not normally need to use this directly, and instead use [left], [top],
     * [right], and [bottom] to return the correct value for the current state.
     */
    val layoutInsets: Insets
        get() = _layoutInsets

    /**
     * The animated insets for this [InsetsType]. These are the insets which are updated from
     * any on-going animations. If there are no animations in progress, the returned [Insets] will
     * be empty.
     *
     * You should not normally need to use this directly, and instead use [left], [top],
     * [right], and [bottom] to return the correct value for the current state.
     */
    val animatedInsets: Insets
        get() = _animatedInsets

    /**
     * The left dimension of the insets in pixels.
     */
    override val left: Int
        get() = (if (animationInProgress) animatedInsets else layoutInsets).left

    /**
     * The top dimension of the insets in pixels.
     */
    override val top: Int
        get() = (if (animationInProgress) animatedInsets else layoutInsets).top

    /**
     * The right dimension of the insets in pixels.
     */
    override val right: Int
        get() = (if (animationInProgress) animatedInsets else layoutInsets).right

    /**
     * The bottom dimension of the insets in pixels.
     */
    override val bottom: Int
        get() = (if (animationInProgress) animatedInsets else layoutInsets).bottom

    /**
     * Whether the insets are currently visible.
     */
    var isVisible by mutableStateOf(true)
        internal set

    /**
     * Whether this insets type is being animated at this moment.
     */
    val animationInProgress: Boolean
        get() = ongoingAnimationsCount > 0

    /**
     * The progress of any ongoing animations, in the range of 0 to 1.
     * If there is no animation in progress, this will return 0.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    var animationFraction by mutableStateOf(0f)
        internal set

    internal fun onAnimationStart() {
        ongoingAnimationsCount++
    }

    internal fun onAnimationEnd() {
        ongoingAnimationsCount--

        if (ongoingAnimationsCount == 0) {
            // If there are no on-going animations, clear out the animated insets
            _animatedInsets.reset()
            animationFraction = 0f
        }
    }
}

@Stable
interface Insets {
    /**
     * The left dimension of these insets in pixels.
     */
    @get:IntRange(from = 0)
    val left: Int

    /**
     * The top dimension of these insets in pixels.
     */
    @get:IntRange(from = 0)
    val top: Int

    /**
     * The right dimension of these insets in pixels.
     */
    @get:IntRange(from = 0)
    val right: Int

    /**
     * The bottom dimension of these insets in pixels.
     */
    @get:IntRange(from = 0)
    val bottom: Int

    fun copy(
        left: Int = this.left,
        top: Int = this.top,
        right: Int = this.right,
        bottom: Int = this.bottom,
    ): Insets = MutableInsets(left, top, right, bottom)

    operator fun minus(other: Insets): Insets = copy(
        left = this.left - other.left,
        top = this.top - other.top,
        right = this.right - other.right,
        bottom = this.bottom - other.bottom,
    )

    operator fun plus(other: Insets): Insets = copy(
        left = this.left + other.left,
        top = this.top + other.top,
        right = this.right + other.right,
        bottom = this.bottom + other.bottom,
    )
}

internal class MutableInsets(
    left: Int = 0,
    top: Int = 0,
    right: Int = 0,
    bottom: Int = 0,
) : Insets {
    override var left by mutableStateOf(left)
        internal set

    override var top by mutableStateOf(top)
        internal set

    override var right by mutableStateOf(right)
        internal set

    override var bottom by mutableStateOf(bottom)
        internal set

    fun reset() {
        left = 0
        top = 0
        right = 0
        bottom = 0
    }
}

/**
 * Composition local containing the current [WindowInsets].
 */
val LocalWindowInsets = staticCompositionLocalOf { WindowInsets() }

/**
 * This class sets up the necessary listeners on the given [view] to be able to observe
 * [WindowInsetsCompat] instances dispatched by the system.
 *
 * This class is useful for when you prefer to handle the ownership of the [WindowInsets]
 * yourself. One example of this is if you find yourself using [ProvideWindowInsets] in fragments.
 *
 * It is convenient to use [ProvideWindowInsets] in fragments, but that can result in a
 * delay in the initial inset update, which results in a visual flicker.
 * See [this issue](https://github.com/google/accompanist/issues/155) for more information.
 *
 * The alternative is for fragments to manage the [WindowInsets] themselves, like so:
 *
 * ```
 * override fun onCreateView(
 *     inflater: LayoutInflater,
 *     container: ViewGroup?,
 *     savedInstanceState: Bundle?
 * ): View = ComposeView(requireContext()).apply {
 *     layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
 *
 *     // Create an ViewWindowInsetObserver using this view
 *     val observer = ViewWindowInsetObserver(this)
 *
 *     // Call start() to start listening now.
 *     // The WindowInsets instance is returned to us.
 *     val windowInsets = observer.start()
 *
 *     setContent {
 *         // Instead of calling ProvideWindowInsets, we use CompositionLocalProvider to provide
 *         // the WindowInsets instance from above to LocalWindowInsets
 *         CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
 *             /* Content */
 *         }
 *     }
 * }
 * ```
 *
 * @param view The view to observe [WindowInsetsCompat]s from.
 */
class ViewWindowInsetObserver(private val view: View) {
    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = v.requestApplyInsets()
        override fun onViewDetachedFromWindow(v: View) = Unit
    }

    /**
     * Whether this [ViewWindowInsetObserver] is currently observing.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var isObserving: Boolean = false
        private set

    /**
     * Start observing window insets from [view]. Make sure to call [stop] if required.
     *
     * @param consumeWindowInsets Whether to consume any [WindowInsetsCompat]s which are
     * dispatched to the host view. Defaults to `true`.
     */
    fun start(
        consumeWindowInsets: Boolean = true
    ): WindowInsets {
        return WindowInsets().apply {
            observeInto(
                windowInsets = this,
                consumeWindowInsets = consumeWindowInsets,
                windowInsetsAnimationsEnabled = false
            )
        }
    }

    /**
     * Start observing window insets from [view]. Make sure to call [stop] if required.
     *
     * @param windowInsetsAnimationsEnabled Whether to listen for [WindowInsetsAnimation]s, such as
     * IME animations.
     * @param consumeWindowInsets Whether to consume any [WindowInsetsCompat]s which are
     * dispatched to the host view. Defaults to `true`.
     */
    @ExperimentalAnimatedInsets
    fun start(
        windowInsetsAnimationsEnabled: Boolean,
        consumeWindowInsets: Boolean = true,
    ): WindowInsets {
        return WindowInsets().apply {
            observeInto(
                windowInsets = this,
                consumeWindowInsets = consumeWindowInsets,
                windowInsetsAnimationsEnabled = windowInsetsAnimationsEnabled
            )
        }
    }

    internal fun observeInto(
        windowInsets: WindowInsets,
        consumeWindowInsets: Boolean,
        windowInsetsAnimationsEnabled: Boolean,
    ) {
        require(!isObserving) {
            "start() called, but this ViewWindowInsetObserver is already observing"
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, wic ->
            // Go through each inset type and update its layoutInsets from the
            // WindowInsetsCompat values
            windowInsets.statusBars.run {
                _layoutInsets.updateFrom(wic.getInsets(WindowInsetsCompat.Type.statusBars()))
                isVisible = wic.isVisible(WindowInsetsCompat.Type.statusBars())
            }
            windowInsets.navigationBars.run {
                _layoutInsets.updateFrom(wic.getInsets(WindowInsetsCompat.Type.navigationBars()))
                isVisible = wic.isVisible(WindowInsetsCompat.Type.navigationBars())
            }
            windowInsets.systemBars.run {
                _layoutInsets.updateFrom(wic.getInsets(WindowInsetsCompat.Type.systemBars()))
                isVisible = wic.isVisible(WindowInsetsCompat.Type.systemBars())
            }
            windowInsets.systemGestures.run {
                _layoutInsets.updateFrom(wic.getInsets(WindowInsetsCompat.Type.systemGestures()))
                isVisible = wic.isVisible(WindowInsetsCompat.Type.systemGestures())
            }
            windowInsets.ime.run {
                _layoutInsets.updateFrom(wic.getInsets(WindowInsetsCompat.Type.ime()))
                isVisible = wic.isVisible(WindowInsetsCompat.Type.ime())
            }

            if (consumeWindowInsets) WindowInsetsCompat.CONSUMED else wic
        }

        // Add an OnAttachStateChangeListener to request an inset pass each time we're attached
        // to the window
        view.addOnAttachStateChangeListener(attachListener)

        if (windowInsetsAnimationsEnabled) {
            ViewCompat.setWindowInsetsAnimationCallback(
                view,
                InnerWindowInsetsAnimationCallback(windowInsets)
            )
        } else {
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }

        if (view.isAttachedToWindow) {
            // If the view is already attached, we can request an inset pass now
            view.requestApplyInsets()
        }

        isObserving = true
    }

    /**
     * Removes any listeners from the [view] so that we no longer observe inset changes.
     *
     * This is only required to be called from hosts which have a shorter lifetime than the [view].
     * For example, if you're using [ViewWindowInsetObserver] from a `@Composable` function,
     * you should call [stop] from an `onDispose` block, like so:
     *
     * ```
     * DisposableEffect(view) {
     *     val observer = ViewWindowInsetObserver(view)
     *     // ...
     *     onDispose {
     *         observer.stop()
     *     }
     * }
     * ```
     *
     * Whereas if you're using this class from a fragment (or similar), it is not required to
     * call this function since it will live as least as longer as the view.
     */
    fun stop() {
        require(isObserving) {
            "stop() called, but this ViewWindowInsetObserver is not currently observing"
        }
        view.removeOnAttachStateChangeListener(attachListener)
        ViewCompat.setOnApplyWindowInsetsListener(view, null)
        isObserving = false
    }
}

/**
 * Applies any [WindowInsetsCompat] values to [LocalWindowInsets], which are then available
 * within [content].
 *
 * If you're using this in fragments, you may wish to take a look at
 * [ViewWindowInsetObserver] for a more optimal solution.
 *
 * @param consumeWindowInsets Whether to consume any [WindowInsetsCompat]s which are dispatched to
 * the host view. Defaults to `true`.
 */
@SuppressLint("UnnecessaryLambdaCreation")
@Composable
fun ProvideWindowInsets(
    consumeWindowInsets: Boolean = true,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val windowInsets = LocalWindowInsets.current

    DisposableEffect(view) {
        val observer = ViewWindowInsetObserver(view)
        observer.observeInto(
            windowInsets = windowInsets,
            consumeWindowInsets = consumeWindowInsets,
            windowInsetsAnimationsEnabled = false
        )
        onDispose {
            observer.stop()
        }
    }

    CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
        content()
    }
}

/**
 * Applies any [WindowInsetsCompat] values to [LocalWindowInsets], which are then available
 * within [content].
 *
 * If you're using this in fragments, you may wish to take a look at
 * [ViewWindowInsetObserver] for a more optimal solution.
 *
 * @param windowInsetsAnimationsEnabled Whether to listen for [WindowInsetsAnimation]s, such as
 * IME animations.
 * @param consumeWindowInsets Whether to consume any [WindowInsetsCompat]s which are dispatched to
 * the host view. Defaults to `true`.
 */
@SuppressLint("UnnecessaryLambdaCreation")
@ExperimentalAnimatedInsets
@Composable
fun ProvideWindowInsets(
    windowInsetsAnimationsEnabled: Boolean,
    consumeWindowInsets: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val windowInsets = remember { WindowInsets() }

    DisposableEffect(view) {
        val observer = ViewWindowInsetObserver(view)
        observer.observeInto(
            windowInsets = windowInsets,
            consumeWindowInsets = consumeWindowInsets,
            windowInsetsAnimationsEnabled = windowInsetsAnimationsEnabled
        )
        onDispose {
            observer.stop()
        }
    }

    CompositionLocalProvider(LocalWindowInsets provides windowInsets) {
        content()
    }
}

private class InnerWindowInsetsAnimationCallback(
    private val windowInsets: WindowInsets,
) : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        // Go through each type and flag that an animation has started
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            windowInsets.ime.onAnimationStart()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.statusBars() != 0) {
            windowInsets.statusBars.onAnimationStart()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.navigationBars() != 0) {
            windowInsets.navigationBars.onAnimationStart()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.systemBars() != 0) {
            windowInsets.systemBars.onAnimationStart()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.systemGestures() != 0) {
            windowInsets.systemGestures.onAnimationStart()
        }
    }

    override fun onProgress(
        platformInsets: WindowInsetsCompat,
        runningAnimations: List<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        // Update each inset type with the given parameters
        windowInsets.ime.updateAnimation(
            platformInsets = platformInsets,
            runningAnimations = runningAnimations,
            type = WindowInsetsCompat.Type.ime()
        )
        windowInsets.statusBars.updateAnimation(
            platformInsets = platformInsets,
            runningAnimations = runningAnimations,
            type = WindowInsetsCompat.Type.statusBars()
        )
        windowInsets.navigationBars.updateAnimation(
            platformInsets = platformInsets,
            runningAnimations = runningAnimations,
            type = WindowInsetsCompat.Type.navigationBars()
        )
        windowInsets.systemBars.updateAnimation(
            platformInsets = platformInsets,
            runningAnimations = runningAnimations,
            type = WindowInsetsCompat.Type.systemBars()
        )
        windowInsets.systemBars.updateAnimation(
            platformInsets = platformInsets,
            runningAnimations = runningAnimations,
            type = WindowInsetsCompat.Type.systemGestures()
        )
        return platformInsets
    }

    private inline fun InsetsType.updateAnimation(
        platformInsets: WindowInsetsCompat,
        runningAnimations: List<WindowInsetsAnimationCompat>,
        type: Int,
    ) {
        // If there are animations of the given type...
        if (runningAnimations.any { it.typeMask or type != 0 }) {
            // Update our animated inset values
            _animatedInsets.updateFrom(platformInsets.getInsets(type))
            // And update the animation fraction. We use the maximum animation progress of any
            // ongoing animations for this type.
            animationFraction = runningAnimations.maxOf { it.fraction }
        }
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        // Go through each type and flag that an animation has ended
        if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
            windowInsets.ime.onAnimationEnd()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.statusBars() != 0) {
            windowInsets.statusBars.onAnimationEnd()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.navigationBars() != 0) {
            windowInsets.navigationBars.onAnimationEnd()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.systemBars() != 0) {
            windowInsets.systemBars.onAnimationEnd()
        }
        if (animation.typeMask and WindowInsetsCompat.Type.systemGestures() != 0) {
            windowInsets.systemGestures.onAnimationEnd()
        }
    }
}

/**
 * Updates our mutable state backed [InsetsType] from an Android system insets.
 */
private fun MutableInsets.updateFrom(insets: androidx.core.graphics.Insets) {
    left = insets.left
    top = insets.top
    right = insets.right
    bottom = insets.bottom
}

/**
 * Ensures that each dimension is not less than corresponding dimension in the
 * specified [minimumValue].
 *
 * @return this if every dimension is greater than or equal to the corresponding
 * dimension value in [minimumValue], otherwise a copy of this with each dimension coerced with the
 * corresponding dimension value in [minimumValue].
 */
fun InsetsType.coerceEachDimensionAtLeast(minimumValue: InsetsType): Insets {
    // Fast path, no need to copy if: this >= minimumValue
    if (left >= minimumValue.left && top >= minimumValue.top &&
        right >= minimumValue.right && bottom >= minimumValue.bottom
    ) {
        return this
    }
    return MutableInsets(
        left = left.coerceAtLeast(minimumValue.left),
        top = top.coerceAtLeast(minimumValue.top),
        right = right.coerceAtLeast(minimumValue.right),
        bottom = bottom.coerceAtLeast(minimumValue.bottom),
    )
}

enum class HorizontalSide { Left, Right }
enum class VerticalSide { Top, Bottom }

@RequiresOptIn(
    message = "Animated Insets support is experimental. The API may be changed in the " +
        "future."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ExperimentalAnimatedInsets
