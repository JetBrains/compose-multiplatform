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

package androidx.compose.foundation

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [clickable] [PressInteraction] behavior in scrollable [ViewGroup]s.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class ClickableInScrollableViewGroupTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clickable_scrollableViewGroup() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        class ScrollingViewGroup(context: Context) : FrameLayout(context) {
            override fun shouldDelayChildPressedState() = true
        }

        rule.activityRule.scenario.onActivity { activity ->
            ComposeView(activity).apply {
                activity.setContentView(ScrollingViewGroup(activity).also { it.addView(this) })
                setContent {
                    scope = rememberCoroutineScope()
                    Box {
                        BasicText(
                            "ClickableText",
                            modifier = Modifier
                                .testTag("myClickable")
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {}
                        )
                    }
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    /**
     * Test case for a [clickable] inside an [AndroidView] inside a scrollable Compose container
     */
    @Test
    fun clickable_androidViewInScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                AndroidView({ context -> ComposeView(context).apply {
                    setContent {
                        Box {
                            BasicText(
                                "ClickableText",
                                modifier = Modifier
                                    .testTag("myClickable")
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {}
                            )
                        }
                    }
                } })
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    /**
     * Test case for a [clickable] inside an [AndroidView] inside a non-scrollable Compose container
     */
    @Ignore(
        "b/203141462 - currently this is not implemented so AndroidView()s will always " +
            "appear scrollable"
    )
    @Test
    fun clickable_androidViewInNotScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                AndroidView({ context -> ComposeView(context).apply {
                    setContent {
                        Box {
                            BasicText(
                                "ClickableText",
                                modifier = Modifier
                                    .testTag("myClickable")
                                    .combinedClickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {}
                            )
                        }
                    }
                } })
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { down(center) }

        // No scrollable container, so there should be no delay and we should instantly appear
        // pressed
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable")
            .performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }
}