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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnCreate
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnRelease
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnReset
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnUpdate
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnViewAttach
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.OnViewDetach
import androidx.compose.ui.viewinterop.AndroidViewTest.AndroidViewLifecycleEvent.ViewLifecycleEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
class AndroidViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun androidViewWithConstructor() {
        rule.setContent {
            AndroidView({ TextView(it).apply { text = "Test" } })
        }
        Espresso
            .onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest() {
        rule.setContent {
            AndroidView({ LayoutInflater.from(it).inflate(R.layout.test_layout, null) })
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithViewTest() {
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        rule.setContent {
            AndroidView({ frameLayout })
        }
        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest_preservesLayoutParams() {
        rule.setContent {
            AndroidView({
                LayoutInflater.from(it).inflate(R.layout.test_layout, FrameLayout(it), false)
            })
        }
        Espresso
            .onView(withClassName(endsWith("RelativeLayout")))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                if (view.layoutParams.width != 300.dp.toPx(view.context.resources.displayMetrics)) {
                    throw exception
                }
                if (view.layoutParams.height != WRAP_CONTENT) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewProperlyDetached() {
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        var emit by mutableStateOf(true)
        rule.setContent {
            if (emit) {
                AndroidView({ frameLayout })
            }
        }

        rule.runOnUiThread {
            assertThat(frameLayout.parent).isNotNull()
            emit = false
        }

        rule.runOnIdle {
            assertThat(frameLayout.parent).isNull()
        }
    }

    @Test
    @LargeTest
    fun androidView_attachedAfterDetached_addsViewBack() {
        lateinit var root: FrameLayout
        lateinit var composeView: ComposeView
        lateinit var viewInsideCompose: View
        rule.activityRule.scenario.onActivity { activity ->
            root = FrameLayout(activity)
            composeView = ComposeView(activity)
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity)
            )
            viewInsideCompose = View(activity)

            activity.setContentView(root)
            root.addView(composeView)
            composeView.setContent {
                AndroidView({ viewInsideCompose })
            }
        }

        var viewInsideComposeHolder: ViewGroup? = null
        rule.runOnUiThread {
            assertThat(viewInsideCompose.parent).isNotNull()
            viewInsideComposeHolder = viewInsideCompose.parent as ViewGroup
            root.removeView(composeView)
        }

        rule.runOnIdle {
            // Views don't detach from the parent when the parent is detached
            assertThat(viewInsideCompose.parent).isNotNull()
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
            root.addView(composeView)
        }

        rule.runOnIdle {
            assertThat(viewInsideCompose.parent).isEqualTo(viewInsideComposeHolder)
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
        }
    }

    @Test
    fun androidViewWithResource_modifierIsApplied() {
        val size = 20.dp
        rule.setContent {
            AndroidView(
                { LayoutInflater.from(it).inflate(R.layout.test_layout, null) },
                Modifier.requiredSize(size)
            )
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewWithView_modifierIsApplied() {
        val size = 20.dp
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity)
        }
        rule.setContent {
            AndroidView({ frameLayout }, Modifier.requiredSize(size))
        }

        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidViewWithView_drawModifierIsApplied() {
        val size = 300
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
        }
        rule.setContent {
            AndroidView({ frameLayout },
                Modifier
                    .testTag("view")
                    .background(color = Color.Blue))
        }

        rule.onNodeWithTag("view").captureToImage().assertPixels(IntSize(size, size)) {
            Color.Blue
        }
    }

    @Test
    fun androidViewWithResource_modifierIsCorrectlyChanged() {
        val size = mutableStateOf(20.dp)
        rule.setContent {
            AndroidView(
                { LayoutInflater.from(it).inflate(R.layout.test_layout, null) },
                Modifier.requiredSize(size.value)
            )
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
        rule.runOnIdle { size.value = 30.dp }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidView_notDetachedFromWindowTwice() {
        // Should not crash.
        rule.setContent {
            Box {
                AndroidView(::ComposeView) {
                    it.setContent {
                        Box(Modifier)
                    }
                }
            }
        }
    }

    @Test
    fun androidView_updateObservesStateChanges() {
        var size by mutableStateOf(20)
        var obtainedSize: IntSize = IntSize.Zero
        rule.setContent {
            Box {
                AndroidView(
                    ::View,
                    Modifier.onGloballyPositioned { obtainedSize = it.size }
                ) { view ->
                    view.layoutParams = ViewGroup.LayoutParams(size, size)
                }
            }
        }
        rule.runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
            size = 40
        }
        rule.runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
        }
    }

    @Test
    fun androidView_propagatesDensity() {
        rule.setContent {
            val size = 50.dp
            val density = Density(3f)
            val sizeIpx = with(density) { size.roundToPx() }
            CompositionLocalProvider(LocalDensity provides density) {
                AndroidView(
                    { FrameLayout(it) },
                    Modifier
                        .requiredSize(size)
                        .onGloballyPositioned {
                            assertThat(it.size).isEqualTo(IntSize(sizeIpx, sizeIpx))
                        }
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun androidView_propagatesViewTreeCompositionContext() {
        lateinit var parentComposeView: ComposeView
        lateinit var compositionChildView: View
        rule.activityRule.scenario.onActivity { activity ->
            parentComposeView = ComposeView(activity).apply {
                setContent {
                    AndroidView(::View) {
                        compositionChildView = it
                    }
                }
                activity.setContentView(this)
            }
        }
        rule.runOnIdle {
            assertThat(compositionChildView.findViewTreeCompositionContext())
                .isNotEqualTo(parentComposeView.findViewTreeCompositionContext())
        }
    }

    @Test
    fun androidView_propagatesLocalsToComposeViewChildren() {
        val ambient = compositionLocalOf { "unset" }
        var childComposedAmbientValue = "uncomposed"
        rule.setContent {
            CompositionLocalProvider(ambient provides "setByParent") {
                AndroidView(
                    factory = {
                        ComposeView(it).apply {
                            setContent {
                                childComposedAmbientValue = ambient.current
                            }
                        }
                    }
                )
            }
        }
        rule.runOnIdle {
            assertThat(childComposedAmbientValue).isEqualTo("setByParent")
        }
    }

    @Test
    fun androidView_propagatesLayoutDirectionToComposeViewChildren() {
        var childViewLayoutDirection: Int = Int.MIN_VALUE
        var childCompositionLayoutDirection: LayoutDirection? = null
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AndroidView(
                    factory = {
                        FrameLayout(it).apply {
                            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                                childViewLayoutDirection = layoutDirection
                            }
                            addView(
                                ComposeView(it).apply {
                                    // The view hierarchy's layout direction should always override
                                    // the ambient layout direction from the parent composition.
                                    layoutDirection = android.util.LayoutDirection.LTR
                                    setContent {
                                        childCompositionLayoutDirection =
                                            LocalLayoutDirection.current
                                    }
                                },
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    }
                )
            }
        }
        rule.runOnIdle {
            assertThat(childViewLayoutDirection).isEqualTo(android.util.LayoutDirection.RTL)
            assertThat(childCompositionLayoutDirection).isEqualTo(LayoutDirection.Ltr)
        }
    }

    @Test
    fun androidView_propagatesLocalLifecycleOwnerAsViewTreeOwner() {
        lateinit var parentLifecycleOwner: LifecycleOwner
        val compositionLifecycleOwner = TestLifecycleOwner()
        var childViewTreeLifecycleOwner: LifecycleOwner? = null

        rule.setContent {
            LocalLifecycleOwner.current.also {
                SideEffect {
                    parentLifecycleOwner = it
                }
            }

            CompositionLocalProvider(LocalLifecycleOwner provides compositionLifecycleOwner) {
                AndroidView(
                    factory = {
                        object : FrameLayout(it) {
                            override fun onAttachedToWindow() {
                                super.onAttachedToWindow()
                                childViewTreeLifecycleOwner = findViewTreeLifecycleOwner()
                            }
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(childViewTreeLifecycleOwner).isSameInstanceAs(compositionLifecycleOwner)
            assertThat(childViewTreeLifecycleOwner).isNotSameInstanceAs(parentLifecycleOwner)
        }
    }

    @Test
    fun androidView_propagatesLocalSavedStateRegistryOwnerAsViewTreeOwner() {
        lateinit var parentSavedStateRegistryOwner: SavedStateRegistryOwner
        val compositionSavedStateRegistryOwner =
            object : SavedStateRegistryOwner, LifecycleOwner by TestLifecycleOwner() {
                // We don't actually need to ever get actual instance.
                override val savedStateRegistry: SavedStateRegistry
                    get() = throw UnsupportedOperationException()
            }
        var childViewTreeSavedStateRegistryOwner: SavedStateRegistryOwner? = null

        rule.setContent {
            LocalSavedStateRegistryOwner.current.also {
                SideEffect {
                    parentSavedStateRegistryOwner = it
                }
            }

            CompositionLocalProvider(
                LocalSavedStateRegistryOwner provides compositionSavedStateRegistryOwner
            ) {
                AndroidView(
                    factory = {
                        object : FrameLayout(it) {
                            override fun onAttachedToWindow() {
                                super.onAttachedToWindow()
                                childViewTreeSavedStateRegistryOwner =
                                    findViewTreeSavedStateRegistryOwner()
                            }
                        }
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(childViewTreeSavedStateRegistryOwner)
                .isSameInstanceAs(compositionSavedStateRegistryOwner)
            assertThat(childViewTreeSavedStateRegistryOwner)
                .isNotSameInstanceAs(parentSavedStateRegistryOwner)
        }
    }

    @Test
    fun androidView_runsFactoryExactlyOnce_afterFirstComposition() {
        var factoryRunCount = 0
        rule.setContent {
            val view = remember { View(rule.activity) }
            AndroidView({ ++factoryRunCount; view })
        }
        rule.runOnIdle {
            assertThat(factoryRunCount).isEqualTo(1)
        }
    }

    @Test
    fun androidView_runsFactoryExactlyOnce_evenWhenFactoryIsChanged() {
        var factoryRunCount = 0
        var first by mutableStateOf(true)
        rule.setContent {
            val view = remember { View(rule.activity) }
            AndroidView(
                if (first) {
                    { ++factoryRunCount; view }
                } else {
                    { ++factoryRunCount; view }
                }
            )
        }
        rule.runOnIdle {
            assertThat(factoryRunCount).isEqualTo(1)
            first = false
        }
        rule.runOnIdle {
            assertThat(factoryRunCount).isEqualTo(1)
        }
    }

    @Ignore
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidView_clipsToBounds() {
        val size = 20
        val sizeDp = with(rule.density) { size.toDp() }
        rule.setContent {
            Column {
                Box(
                    Modifier
                        .size(sizeDp)
                        .background(Color.Blue)
                        .testTag("box"))
                AndroidView(factory = { SurfaceView(it) })
            }
        }

        rule.onNodeWithTag("box").captureToImage().assertPixels(IntSize(size, size)) {
            Color.Blue
        }
    }

    @Test
    fun androidView_restoresState() {
        var result = ""

        @Composable
        fun <T : Any> Navigation(
            currentScreen: T,
            modifier: Modifier = Modifier,
            content: @Composable (T) -> Unit
        ) {
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier) {
                saveableStateHolder.SaveableStateProvider(currentScreen) {
                    content(currentScreen)
                }
            }
        }

        var screen by mutableStateOf("screen1")
        rule.setContent {
            Navigation(screen) { currentScreen ->
                if (currentScreen == "screen1") {
                    AndroidView({
                        StateSavingView(
                            "testKey",
                            "testValue",
                            { restoredValue -> result = restoredValue },
                            it
                        )
                    })
                } else {
                    Box(Modifier)
                }
            }
        }

        rule.runOnIdle { screen = "screen2" }
        rule.runOnIdle { screen = "screen1" }
        rule.runOnIdle {
            assertThat(result).isEqualTo("testValue")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidView_noClip() {
        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)) {
                with(LocalDensity.current) {
                    Box(
                        Modifier
                            .requiredSize(150.toDp())
                            .testTag("box")) {
                        Box(
                            Modifier
                                .size(100.toDp(), 100.toDp())
                                .align(AbsoluteAlignment.TopLeft)
                        ) {
                            AndroidView(factory = { context ->
                                object : View(context) {
                                    init {
                                        clipToOutline = false
                                    }

                                    override fun onDraw(canvas: Canvas) {
                                        val paint = Paint()
                                        paint.color = Color.Blue.toArgb()
                                        paint.style = Paint.Style.FILL
                                        canvas.drawRect(0f, 0f, 150f, 150f, paint)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("box").captureToImage().assertPixels(IntSize(150, 150)) {
            Color.Blue
        }
    }

    @Test
    fun testInitialComposition_causesViewToBecomeActive() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            ReusableContent("never-changes") {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )
    }

    @Test
    fun testViewRecomposition_onlyInvokesUpdate() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var state by mutableStateOf(0)
        rule.setContent {
            ReusableContent("never-changes") {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it) },
                    update = { it.text = "Text $state" },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Text 0")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        state++

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Text 1")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when recomposed",
            listOf(OnUpdate),
            lifecycleEvents
        )
    }

    @Test
    fun testViewDeactivation_causesViewResetAndDetach() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attached by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(attached) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        attached = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents
        )
    }

    @Test
    fun testViewReattachment_causesViewToBecomeReusedAndReactivated() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attached by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(attached) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        attached = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        attached = true

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "reattached to the composition hierarchy",
            listOf(OnViewAttach, OnUpdate),
            lifecycleEvents
        )
    }

    @Test
    fun testViewDisposalWhenDetached_causesViewToBeReleased() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var active by mutableStateOf(true)
        var emit by mutableStateOf(true)
        rule.setContent {
            if (emit) {
                ReusableContentHost(active) {
                    ReusableAndroidViewWithLifecycleTracking(
                        factory = { TextView(it).apply { text = "Test" } },
                        onLifecycleEvent = lifecycleEvents::add
                    )
                }
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        active = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        emit = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy while deactivated",
            listOf(OnRelease),
            lifecycleEvents
        )
    }

    @Test
    fun testViewRemovedFromComposition_causesViewToBeReleased() {
        var includeViewInComposition by mutableStateOf(true)
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            if (includeViewInComposition) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        includeViewInComposition = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from composition while visible",
            listOf(OnViewDetach, OnRelease),
            lifecycleEvents
        )
    }

    @Test
    fun testViewReusedInComposition_invokesReuseCallbackSequence() {
        var key by mutableStateOf(0)
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            ReusableContent(key) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it) },
                    update = { it.text = "Test" },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Test")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        key++

        onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Test")))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "reused in composition",
            listOf(OnReset, OnUpdate),
            lifecycleEvents
        )
    }

    @Test
    fun testViewInComposition_experiencesHostLifecycle_andDoesNotRecreateView() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        rule.setContent {
            ReusableContentHost(active = true) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()

        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.runOnIdle { /* Ensure lifecycle callbacks propagate */ }

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "its host transitioned from RESUMED to CREATED while the view was attached",
            listOf(
                ViewLifecycleEvent(ON_PAUSE),
                ViewLifecycleEvent(ON_STOP)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle { /* Ensure lifecycle callbacks propagate */ }

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "its host transitioned from CREATED to RESUMED while the view was attached",
            listOf(
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )
    }

    @Test
    fun testReactivationWithChangingKey_onlyResetsOnce() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attach by mutableStateOf(true)
        var key by mutableStateOf(1)
        rule.setContent {
            ReusableContentHost(active = attach) {
                ReusableContent(key = key) {
                    ReusableAndroidViewWithLifecycleTracking(
                        factory = { TextView(it).apply { text = "Test" } },
                        onLifecycleEvent = lifecycleEvents::add
                    )
                }
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        attach = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "detached from the composition hierarchy",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        rule.runOnUiThread {
            // Make sure both changes are applied in the same composition.
            attach = true
            key++
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "simultaneously reactivating and changing reuse keys",
            listOf(OnViewAttach, OnUpdate),
            lifecycleEvents
        )
    }

    @Test
    fun testViewDetachedFromComposition_stillExperiencesHostLifecycle() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var attached by mutableStateOf(true)
        rule.setContent {
            ReusableContentHost(attached) {
                ReusableAndroidViewWithLifecycleTracking(
                    factory = { TextView(it).apply { text = "Test" } },
                    onLifecycleEvent = lifecycleEvents::add
                )
            }
        }

        onView(instanceOf(TextView::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        attached = false

        onView(instanceOf(TextView::class.java))
            .check(doesNotExist())

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "removed from the composition hierarchy and retained by Compose",
            listOf(OnReset, OnViewDetach),
            lifecycleEvents
        )
        lifecycleEvents.clear()

        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.runOnIdle { /* Ensure lifecycle callbacks propagate */ }

        assertEquals(
            "AndroidView did not receive callbacks when its host transitioned from " +
                "RESUMED to CREATED while the view was detached",
            listOf(
                ViewLifecycleEvent(ON_PAUSE),
                ViewLifecycleEvent(ON_STOP)
            ),
            lifecycleEvents
        )

        lifecycleEvents.clear()
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle { /* Wait for UI to settle */ }

        assertEquals(
            "AndroidView did not receive callbacks when its host transitioned from " +
                "CREATED to RESUMED while the view was detached",
            listOf(
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )
    }

    @Test
    fun testViewIsReused_whenMoved() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var slotWithContent by mutableStateOf(0)

        rule.setContent {
            val movableContext = remember {
                movableContentOf {
                    ReusableAndroidViewWithLifecycleTracking(
                        factory = {
                            EditText(it).apply { id = R.id.testContentViewId }
                        },
                        onLifecycleEvent = lifecycleEvents::add
                    )
                }
            }

            Column {
                repeat(10) { slot ->
                    if (slot == slotWithContent) {
                        ReusableContent(Unit) {
                            movableContext()
                        }
                    } else {
                        Text("Slot $slot")
                    }
                }
            }
        }

        onView(instanceOf(EditText::class.java))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .perform(typeText("Input"))

        assertEquals(
            "AndroidView did not experience the expected lifecycle when " +
                "added to the composition hierarchy",
            listOf(
                OnCreate,
                OnUpdate,
                OnViewAttach,
                ViewLifecycleEvent(ON_CREATE),
                ViewLifecycleEvent(ON_START),
                ViewLifecycleEvent(ON_RESUME)
            ),
            lifecycleEvents
        )
        lifecycleEvents.clear()
        slotWithContent++

        rule.runOnIdle { /* Wait for UI to settle */ }

        assertEquals(
            "AndroidView experienced unexpected lifecycle events when " +
                "moved in the composition",
            emptyList<AndroidViewLifecycleEvent>(),
            lifecycleEvents
        )

        // Check that the state of the view is retained
        onView(instanceOf(EditText::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("Input")))
    }

    @Test
    fun testViewRestoresState_whenRemovedAndRecreatedWithNoReuse() {
        val lifecycleEvents = mutableListOf<AndroidViewLifecycleEvent>()
        var screen by mutableStateOf("screen1")
        rule.setContent {
            with(rememberSaveableStateHolder()) {
                if (screen == "screen1") {
                    SaveableStateProvider("screen1") {
                        ReusableAndroidViewWithLifecycleTracking(
                            factory = {
                                EditText(it).apply { id = R.id.testContentViewId }
                            },
                            onLifecycleEvent = lifecycleEvents::add
                        )
                    }
                }
            }
        }

        onView(instanceOf(EditText::class.java))
            .check(matches(isDisplayed()))
            .perform(typeText("User Input"))

        rule.runOnIdle { screen = "screen2" }

        onView(instanceOf(EditText::class.java))
            .check(doesNotExist())

        rule.runOnIdle { screen = "screen1" }

        onView(instanceOf(EditText::class.java))
            .check(matches(isDisplayed()))
            .check(matches(withText("User Input")))
    }

    @ExperimentalComposeUiApi
    @Composable
    private inline fun <T : View> ReusableAndroidViewWithLifecycleTracking(
        crossinline factory: (Context) -> T,
        noinline onLifecycleEvent: @DisallowComposableCalls (AndroidViewLifecycleEvent) -> Unit,
        modifier: Modifier = Modifier,
        crossinline update: (T) -> Unit = { },
        crossinline reuse: (T) -> Unit = { },
        crossinline release: (T) -> Unit = { }
    ) {
        AndroidView(
            factory = {
                onLifecycleEvent(OnCreate)
                factory(it).apply {
                    addOnAttachStateChangeListener(
                        object : OnAttachStateChangeListener, LifecycleEventObserver {
                            override fun onViewAttachedToWindow(v: View) {
                                onLifecycleEvent(OnViewAttach)
                                findViewTreeLifecycleOwner()!!.lifecycle.addObserver(this)
                            }

                            override fun onViewDetachedFromWindow(v: View) {
                                onLifecycleEvent(OnViewDetach)
                            }

                            override fun onStateChanged(
                                source: LifecycleOwner,
                                event: Lifecycle.Event
                            ) {
                                onLifecycleEvent(ViewLifecycleEvent(event))
                            }
                        }
                    )
                }
            },
            modifier = modifier,
            update = {
                onLifecycleEvent(OnUpdate)
                update(it)
            },
            onReset = {
                onLifecycleEvent(OnReset)
                reuse(it)
            },
            onRelease = {
                onLifecycleEvent(OnRelease)
                release(it)
            }
        )
    }

    private sealed class AndroidViewLifecycleEvent {
        override fun toString(): String {
            return javaClass.simpleName
        }

        // Sent when the factory lambda is invoked
        object OnCreate : AndroidViewLifecycleEvent()

        object OnUpdate : AndroidViewLifecycleEvent()
        object OnReset : AndroidViewLifecycleEvent()
        object OnRelease : AndroidViewLifecycleEvent()

        object OnViewAttach : AndroidViewLifecycleEvent()
        object OnViewDetach : AndroidViewLifecycleEvent()

        data class ViewLifecycleEvent(
            val event: Lifecycle.Event
        ) : AndroidViewLifecycleEvent() {
            override fun toString() = "ViewLifecycleEvent($event)"
        }
    }

    private class StateSavingView(
        private val key: String,
        private val value: String,
        private val onRestoredValue: (String) -> Unit,
        context: Context
    ) : View(context) {
        init {
            id = 73
        }

        override fun onSaveInstanceState(): Parcelable {
            val superState = super.onSaveInstanceState()
            val bundle = Bundle()
            bundle.putParcelable("superState", superState)
            bundle.putString(key, value)
            return bundle
        }

        @Suppress("DEPRECATION")
        override fun onRestoreInstanceState(state: Parcelable?) {
            super.onRestoreInstanceState((state as Bundle).getParcelable("superState"))
            onRestoredValue(state.getString(key)!!)
        }
    }

    private fun Dp.toPx(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            displayMetrics
        ).roundToInt()
}
