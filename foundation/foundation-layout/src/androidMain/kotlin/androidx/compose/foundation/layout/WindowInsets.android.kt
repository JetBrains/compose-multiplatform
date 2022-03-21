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

package androidx.compose.foundation.layout

import androidx.core.graphics.Insets as AndroidXInsets
import android.os.Build
import android.view.View
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import java.util.WeakHashMap
import androidx.compose.ui.R
import org.jetbrains.annotations.TestOnly

internal fun AndroidXInsets.toInsetsValues(): InsetsValues =
    InsetsValues(left, top, right, bottom)

internal fun ValueInsets(insets: AndroidXInsets, name: String): ValueInsets =
    ValueInsets(insets.toInsetsValues(), name)

/**
 * Indicates whether access to [WindowInsets] within the [content][ComposeView.setContent]
 * should consume the Android  [android.view.WindowInsets]. The default value is `true`, meaning
 * that access to [WindowInsets.Companion] will consume the Android WindowInsets.
 *
 * This property should be set prior to first composition.
 */
var ComposeView.consumeWindowInsets: Boolean
    get() = getTag(R.id.consume_window_insets_tag) as? Boolean ?: true
    set(value) {
        setTag(R.id.consume_window_insets_tag, value)
    }

/**
 * For the [WindowInsetsCompat.Type.captionBar].
 */
val WindowInsets.Companion.captionBar: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().captionBar

/**
 * For the [WindowInsetsCompat.Type.displayCutout]. This insets represents the area that the
 * display cutout (e.g. for camera) is and important content should be excluded from.
 */
val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().displayCutout

/**
 * For the [WindowInsetsCompat.Type.ime]. On [Build.VERSION_CODES.R] and above, the
 * soft keyboard can be detected and [ime] will animate when it shows.
 *
 * Developers should set `android:windowSoftInputMode="adjustResize"` in their
 * `AndroidManifest.xml` file and call `WindowCompat.setDecorFitsSystemWindows(window, false)`
 * in their [android.app.Activity.onCreate].
 */
val WindowInsets.Companion.ime: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().ime

/**
 * For the [WindowInsetsCompat.Type.mandatorySystemGestures]. These insets represents the
 * space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().mandatorySystemGestures

/**
 * For the [WindowInsetsCompat.Type.navigationBars]. These insets represent where
 * system UI places navigation bars. Interactive UI should avoid the navigation bars
 * area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().navigationBars

/**
 * For the [WindowInsetsCompat.Type.statusBars].
 */
val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().statusBars

/**
 * For the [WindowInsetsCompat.Type.systemBars].
 */
val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemBars

/**
 * For the [WindowInsetsCompat.Type.systemGestures].
 */
val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemGestures

/**
 * For the [WindowInsetsCompat.Type.tappableElement].
 */
val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().tappableElement

/**
 * The insets for the curved areas in a waterfall display.
 */
val WindowInsets.Companion.waterfall: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().waterfall

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [system bars][systemBars], [display cutout][displayCutout], and
 * [soft keyboard][ime].
 */
val WindowInsets.Companion.safeDrawing: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().safeDrawing

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [system gestures][systemGestures],
 * [mandatory system gestures][mandatorySystemGestures],
 * [rounded display areas][waterfall], and [tappable areas][tappableElement].
 */
val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().safeGestures

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
val WindowInsets.Companion.safeContent: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().safeContent

/**
 * The insets that the [WindowInsetsCompat.Type.captionBar] will consume if shown.
 * If it cannot be shown then this will be empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.captionBarIgnoringVisibility: WindowInsets
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().captionBarIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.navigationBars] will consume if shown.
 * These insets represent where system UI places navigation bars. Interactive UI should
 * avoid the navigation bars area. If navigation bars cannot be shown, then this will be
 * empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.navigationBarsIgnoringVisibility: WindowInsets
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().navigationBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.statusBars] will consume if shown.
 * If the status bar can never be shown, then this will be empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.statusBarsIgnoringVisibility: WindowInsets
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().statusBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.systemBars] will consume if shown.
 *
 * If system bars can never be shown, then this will be empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.systemBarsIgnoringVisibility: WindowInsets
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.tappableElement] will consume if active.
 *
 * If there are never tappable elements then this is empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.tappableElementIgnoringVisibility: WindowInsets
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().tappableElementIgnoringVisibility

/**
 * `true` when the [caption bar][captionBar] is being displayed, irrespective of
 * whether it intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.isCaptionBarVisible: Boolean
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().isCaptionBarVisible

/**
 * `true` when the [soft keyboard][ime] is being displayed, irrespective of
 * whether it intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.isImeVisible: Boolean
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().isImeVisible

/**
 * `true` when the [statusBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.areStatusBarsVisible: Boolean
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().areStatusBarsVisible

/**
 * `true` when the [navigationBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.areNavigationBarsVisible: Boolean
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().areNavigationBarsVisible

/**
 * `true` when the [systemBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.areSystemBarsVisible: Boolean
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().areSystemBarsVisible
/**
 * `true` when the [tappableElement] is being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.isTappableElementVisible: Boolean
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().isTappableElementVisible

/**
 * The insets for various values in the current window.
 */
internal class WindowInsetsHolder private constructor(insets: WindowInsetsCompat?) {
    val captionBar =
        valueInsets(insets, WindowInsetsCompat.Type.captionBar(), "captionBar")
    val displayCutout =
        valueInsets(insets, WindowInsetsCompat.Type.displayCutout(), "displayCutout")
    val ime = valueInsets(insets, WindowInsetsCompat.Type.ime(), "ime")
    val mandatorySystemGestures = valueInsets(
        insets,
        WindowInsetsCompat.Type.mandatorySystemGestures(),
        "mandatorySystemGestures"
    )
    val navigationBars =
        valueInsets(insets, WindowInsetsCompat.Type.navigationBars(), "navigationBars")
    val statusBars =
        valueInsets(insets, WindowInsetsCompat.Type.statusBars(), "statusBars")
    val systemBars =
        valueInsets(insets, WindowInsetsCompat.Type.systemBars(), "systemBars")
    val systemGestures =
        valueInsets(insets, WindowInsetsCompat.Type.systemGestures(), "systemGestures")
    val tappableElement =
        valueInsets(insets, WindowInsetsCompat.Type.tappableElement(), "tappableElement")
    val waterfall =
        ValueInsets(insets?.displayCutout?.waterfallInsets ?: AndroidXInsets.NONE, "waterfall")
    val safeDrawing =
        systemBars.union(ime).union(displayCutout)
    val safeGestures: WindowInsets =
        tappableElement.union(mandatorySystemGestures).union(systemGestures).union(waterfall)
    val safeContent: WindowInsets = safeDrawing.union(safeGestures)

    val captionBarIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.captionBar(),
        "captionBarIgnoringVisibility"
    )
    val navigationBarsIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets, WindowInsetsCompat.Type.navigationBars(), "navigationBarsIgnoringVisibility"
    )
    val statusBarsIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.statusBars(),
        "statusBarsIgnoringVisibility"
    )
    val systemBarsIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.systemBars(),
        "systemBarsIgnoringVisibility"
    )
    val tappableElementIgnoringVisibility = valueInsetsIgnoringVisibility(
        insets,
        WindowInsetsCompat.Type.tappableElement(),
        "tappableElementIgnoringVisibility"
    )

    var isCaptionBarVisible by mutableStateIsVisible(insets, WindowInsetsCompat.Type.captionBar())
    var isImeVisible by mutableStateIsVisible(insets, WindowInsetsCompat.Type.ime())
    var areNavigationBarsVisible by mutableStateIsVisible(
        insets,
        WindowInsetsCompat.Type.navigationBars()
    )
    var areStatusBarsVisible by mutableStateIsVisible(insets, WindowInsetsCompat.Type.statusBars())
    var areSystemBarsVisible by mutableStateIsVisible(insets, WindowInsetsCompat.Type.systemBars())
    var isTappableElementVisible by mutableStateIsVisible(
        insets,
        WindowInsetsCompat.Type.tappableElement()
    )

    /**
     * `true` unless the `ComposeView` [ComposeView.consumeWindowInsets] is set to `false`.
     */
    var consumes = true

    /**
     * The number of accesses to [WindowInsetsHolder]. When this reaches
     * zero, the listeners are removed. When it increases to 1, the listeners are added.
     */
    private var accessCount = 0

    private val insetsListener = InsetsListener(this)

    /**
     * A usage of [WindowInsetsHolder.current] was added. We must track so that when the
     * first one is added, listeners are set and when the last is removed, the listeners
     * are removed.
     */
    fun incrementAccessors(view: View) {
        if (accessCount == 0) {
            // add listeners
            ViewCompat.setOnApplyWindowInsetsListener(view, insetsListener)

            // We don't need animation callbacks on earlier versions, so don't bother adding
            // the listener. ViewCompat calls the animation callbacks superfluously.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setWindowInsetsAnimationCallback(view, insetsListener)
            }
        }
        accessCount++
    }

    /**
     * A usage of [WindowInsetsHolder.current] was removed. We must track so that when the
     * first one is added, listeners are set and when the last is removed, the listeners
     * are removed.
     */
    fun decrementAccessors(view: View) {
        accessCount--
        if (accessCount == 0) {
            // remove listeners
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
            ViewCompat.setWindowInsetsAnimationCallback(view, null)
        }
    }

    /**
     * Updates the WindowInsets values and notifies changes.
     */
    fun update(windowInsets: WindowInsetsCompat) {
        Snapshot.withMutableSnapshot {
            val insets = if (testInsets) {
                // WindowInsetsCompat erases insets that aren't part of the device.
                // For example, if there is no navigation bar because of hardware keys,
                // the bottom navigation bar will be removed. By using the constructor
                // that doesn't accept a View, it doesn't remove the insets that aren't
                // possible. This is important for testing on arbitrary hardware.
                WindowInsetsCompat.toWindowInsetsCompat(windowInsets.toWindowInsets()!!)
            } else {
                windowInsets
            }
            captionBar.value =
                insets.getInsets(WindowInsetsCompat.Type.captionBar()).toInsetsValues()
            captionBarIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.captionBar()
            ).toInsetsValues()
            isCaptionBarVisible = insets.isVisible(WindowInsetsCompat.Type.captionBar())
            ime.value =
                insets.getInsets(WindowInsetsCompat.Type.ime()).toInsetsValues()
            isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            displayCutout.value =
                insets.getInsets(WindowInsetsCompat.Type.displayCutout()).toInsetsValues()
            navigationBars.value =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars()).toInsetsValues()
            navigationBarsIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.navigationBars()
            ).toInsetsValues()
            areNavigationBarsVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
            statusBars.value =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).toInsetsValues()
            statusBarsIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.statusBars()
            ).toInsetsValues()
            areStatusBarsVisible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
            systemBars.value =
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).toInsetsValues()
            systemBarsIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars()
            ).toInsetsValues()
            areSystemBarsVisible = insets.isVisible(WindowInsetsCompat.Type.systemBars())
            systemGestures.value =
                insets.getInsets(WindowInsetsCompat.Type.systemGestures()).toInsetsValues()
            tappableElement.value =
                insets.getInsets(WindowInsetsCompat.Type.tappableElement()).toInsetsValues()
            tappableElementIgnoringVisibility.value = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.tappableElement()
            ).toInsetsValues()
            isTappableElementVisible = insets.isVisible(WindowInsetsCompat.Type.tappableElement())
            mandatorySystemGestures.value =
                insets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures()).toInsetsValues()

            val cutout = insets.displayCutout
            if (cutout != null) {
                val waterfallInsets = cutout.waterfallInsets
                waterfall.value = waterfallInsets.toInsetsValues()
            }
        }
    }

    companion object {
        /**
         * A mapping of AndroidComposeView to ComposeWindowInsets. Normally a tag is a great
         * way to do this mapping, but off-UI thread and multithreaded composition don't
         * allow using the tag.
         */
        private val viewMap = WeakHashMap<View, WindowInsetsHolder>()

        private var testInsets = false

        /**
         * Testing Window Insets is difficult, so we have this to help eliminate device-specifics
         * from the WindowInsets. This is indirect because `@TestOnly` cannot be applied to a
         * property with a backing field.
         */
        @TestOnly
        fun setUseTestInsets(testInsets: Boolean) {
            this.testInsets = testInsets
        }

        @Composable
        fun current(): WindowInsetsHolder {
            val view = LocalView.current
            val insets = getOrCreateFor(view)

            DisposableEffect(insets) {
                insets.incrementAccessors(view)
                insets.consumes = (view.parent as? View)?.getTag(R.id.consume_window_insets_tag)
                    as? Boolean ?: true
                onDispose {
                    insets.decrementAccessors(view)
                }
            }
            return insets
        }

        /**
         * Returns the [WindowInsetsHolder] associated with [view] or creates one and associates
         * it.
         */
        private fun getOrCreateFor(view: View): WindowInsetsHolder {
            return synchronized(viewMap) {
                viewMap.getOrPut(view) {
                    val insets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        RootWindowInsetsApi23.rootWindowInsets(view)
                    } else {
                        null
                    }
                    WindowInsetsHolder(insets)
                }
            }
        }

        /**
         * Creates a [ValueInsets] using the value from [windowInsets] if it isn't `null`
         */
        private fun valueInsets(
            windowInsets: WindowInsetsCompat?,
            type: Int,
            name: String
        ): ValueInsets {
            val initial = windowInsets?.getInsets(type) ?: AndroidXInsets.NONE
            return ValueInsets(initial, name)
        }

        /**
         * Creates a [ValueInsets] using the "ignoring visibility" value from [windowInsets]
         * if it isn't `null`
         */
        private fun valueInsetsIgnoringVisibility(
            windowInsets: WindowInsetsCompat?,
            type: Int,
            name: String
        ): ValueInsets {
            val initial = windowInsets?.getInsetsIgnoringVisibility(type) ?: AndroidXInsets.NONE
            return ValueInsets(initial, name)
        }

        /**
         * Creates a [ValueInsets] using the "ignoring visibility" value from [windowInsets]
         * if it isn't `null`
         */
        private fun mutableStateIsVisible(
            windowInsets: WindowInsetsCompat?,
            type: Int
        ): MutableState<Boolean> {
            val initial = windowInsets?.isVisible(type) ?: true
            return mutableStateOf(initial)
        }
    }
}

/**
 * Used to get the [View.getRootWindowInsets] only on M and above
 */
@RequiresApi(Build.VERSION_CODES.M)
private object RootWindowInsetsApi23 {
    @DoNotInline
    fun rootWindowInsets(view: View): WindowInsetsCompat? {
        return view.rootWindowInsets?.let {
            WindowInsetsCompat.toWindowInsetsCompat(it, view)
        }
    }
}

private class InsetsListener(
    val composeInsets: WindowInsetsHolder,
) : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP), OnApplyWindowInsetsListener {

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        composeInsets.update(insets)
        return if (composeInsets.consumes) WindowInsetsCompat.CONSUMED else insets
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        composeInsets.update(insets)
        return if (composeInsets.consumes) WindowInsetsCompat.CONSUMED else insets
    }
}
