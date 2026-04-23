package org.jetbrains.compose.components.input

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest as baseRunComposeUiTest
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class PointerClickableTest {

    // -------------------------------------------------------------------------
    // Semantics
    // -------------------------------------------------------------------------

    @Test
    fun semantics_defaultRole_isExposedWhenSet() = runComposeUiTest {
        var role by mutableStateOf<Role?>(Role.Button)

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(role = role) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target")
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        role = null
        waitForIdle()

        // When role is null the property should be absent, not set to null
        onNodeWithTag("target")
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun semantics_disabledState_isReflectedCorrectly() = runComposeUiTest {
        var enabled by mutableStateOf(true)

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(enabled = enabled) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target").assertIsEnabled().assertHasClickAction()

        enabled = false
        waitForIdle()

        onNodeWithTag("target").assertIsNotEnabled().assertHasClickAction()
    }

    @Test
    fun semantics_onClickLabel_isExposedAndUpdatable() = runComposeUiTest {
        var label by mutableStateOf<String?>("open settings")

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(onClickLabel = label) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target").assert(
            SemanticsMatcher("onClick label == 'open settings'") { node ->
                node.config.getOrNull(SemanticsActions.OnClick)?.label == "open settings"
            }
        )

        label = null
        waitForIdle()

        onNodeWithTag("target").assert(
            SemanticsMatcher("onClick label is null") { node ->
                node.config.getOrNull(SemanticsActions.OnClick)?.label == null
            }
        )
    }

    @Test
    fun semantics_roleChangeDuringRecomposition_updatesNode() = runComposeUiTest {
        var role by mutableStateOf<Role?>(Role.Button)

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(role = role) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))

        role = Role.Checkbox
        waitForIdle()

        onNodeWithTag("target")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    // -------------------------------------------------------------------------
    // Primary click
    // -------------------------------------------------------------------------

    @Test
    fun primaryClick_mouse_providesPrimaryButtonMetadata() = runComposeUiTest {
        var event: PointerClickEvent? = null

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick { clickEvent -> event = clickEvent }
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }

        waitForIdle()
        val clickEvent = assertNotNull(event)
        assertNotNull(clickEvent.buttons)
        assertTrue(clickEvent.buttons!!.isPrimaryPressed)
        assertTrue(clickEvent.isPrimaryAction)
        assertFalse(clickEvent.isSecondaryAction)
        assertFalse(clickEvent.isTertiaryAction)
    }

    @Test
    fun primaryClick_touch_isPrimaryAction() = runComposeUiTest {
        var event: PointerClickEvent? = null

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick { clickEvent -> event = clickEvent }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        waitForIdle()
        val clickEvent = assertNotNull(event)
        assertTrue(clickEvent.isPrimaryAction)
        assertFalse(clickEvent.isSecondaryAction)
        assertFalse(clickEvent.isTertiaryAction)
    }

    @Test
    fun primaryClick_multipleSuccessive_eachFiresCallback() = runComposeUiTest {
        var clicks = 0

        setContent {
            Box(Modifier.testTag("target").size(40.dp).onPointerClick { _: PointerClickEvent -> clicks++ })
        }

        repeat(3) {
            onNodeWithTag("target").performTouchInput {
                down(Offset(10f, 10f))
                up()
            }
            waitForIdle()
        }

        assertEquals(3, clicks)
    }

    // -------------------------------------------------------------------------
    // Secondary click
    // -------------------------------------------------------------------------

    @Test
    fun secondaryClick_reportsSecondaryButtonMetadata() = runComposeUiTest {
        val events = mutableListOf<PointerClickEvent>()

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick { clickEvent -> events += clickEvent }
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }

        waitForIdle()

        assertEquals(1, events.size)
        assertTrue(events[0].buttons?.isSecondaryPressed == true)
        assertTrue(events[0].isSecondaryAction)
        assertFalse(events[0].isPrimaryAction)
        assertFalse(events[0].isTertiaryAction)
    }

    @Test
    fun onPrimaryClick_secondaryClick_doesNotFireCallback() = runComposeUiTest {
        // onPrimaryClick must only fire on primary actions.
        var primaryCount = 0

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPrimaryClick { primaryCount++ }
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }

        waitForIdle()
        assertEquals(0, primaryCount)
    }

    @Test
    fun onPrimaryClick_primaryClick_firesCallback() = runComposeUiTest {
        var primaryCount = 0

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPrimaryClick { primaryCount++ }
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }

        waitForIdle()
        assertEquals(1, primaryCount)
    }

    // -------------------------------------------------------------------------
    // Tertiary (middle-mouse) click
    // -------------------------------------------------------------------------

    @Test
    fun tertiaryClick_reportsMiddleButtonMetadata() = runComposeUiTest {
        var event: PointerClickEvent? = null

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick { clickEvent -> event = clickEvent }
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Tertiary)
            release(MouseButton.Tertiary)
        }

        waitForIdle()
        val clickEvent = assertNotNull(event)
        assertTrue(clickEvent.buttons?.isTertiaryPressed == true)
        assertTrue(clickEvent.isTertiaryAction)
        assertFalse(clickEvent.isPrimaryAction)
        assertFalse(clickEvent.isSecondaryAction)
    }

    @Test
    fun onPrimaryClick_tertiaryClick_doesNotFireCallback() = runComposeUiTest {
        var primaryCount = 0

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPrimaryClick { primaryCount++ }
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Tertiary)
            release(MouseButton.Tertiary)
        }

        waitForIdle()
        assertEquals(0, primaryCount)
    }

    // -------------------------------------------------------------------------
    // Keyboard modifiers
    // -------------------------------------------------------------------------

    @Test
    fun keyboardModifiers_areCapturedFromPointerEvent() = runComposeUiTest {
        val events = mutableListOf<PointerClickEvent>()

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick { clickEvent -> events += clickEvent }
            )
        }

        onNodeWithTag("target").performKeyInput { keyDown(androidx.compose.ui.input.key.Key.CtrlLeft) }
        onNodeWithTag("target").performMouseInput { press(MouseButton.Primary); release(MouseButton.Primary) }
        onNodeWithTag("target").performKeyInput { keyUp(androidx.compose.ui.input.key.Key.CtrlLeft) }

        onNodeWithTag("target").performKeyInput { keyDown(androidx.compose.ui.input.key.Key.AltLeft) }
        onNodeWithTag("target").performMouseInput { press(MouseButton.Primary); release(MouseButton.Primary) }
        onNodeWithTag("target").performKeyInput { keyUp(androidx.compose.ui.input.key.Key.AltLeft) }

        onNodeWithTag("target").performKeyInput { keyDown(androidx.compose.ui.input.key.Key.MetaLeft) }
        onNodeWithTag("target").performMouseInput { press(MouseButton.Primary); release(MouseButton.Primary) }
        onNodeWithTag("target").performKeyInput { keyUp(androidx.compose.ui.input.key.Key.MetaLeft) }

        waitForIdle()

        assertEquals(3, events.size)
        assertTrue(events[0].keyboardModifiers.isCtrlPressed)
        assertTrue(events[1].keyboardModifiers.isAltPressed)
        assertTrue(events[2].keyboardModifiers.isMetaPressed)
    }

    // -------------------------------------------------------------------------
    // Click position
    // -------------------------------------------------------------------------

    @Test
    fun clickPosition_usesUpEventPosition_notDownPosition() = runComposeUiTest {
        // The PointerClickEvent.position must reflect where the finger/cursor
        // was when it was released, not where it was pressed.
        var event: PointerClickEvent? = null

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(100.dp)
                    .onPointerClick { clickEvent -> event = clickEvent }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))   // press at (10, 10)
            moveTo(Offset(60f, 60f)) // drag within bounds
            up()                     // release at (60, 60)
        }

        waitForIdle()
        assertEquals(Offset(60f, 60f), assertNotNull(event).position)
    }

    @Test
    fun clickPosition_exactEdge_isStillInsideBounds() = runComposeUiTest {
        var event: PointerClickEvent? = null

        setContent {
            Box(Modifier.size(40.dp).testTag("target").onPointerClick { clickEvent -> event = clickEvent })
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(1f, 20f))
            moveTo(Offset(40f, 20f))
            up()
        }

        waitForIdle()
        assertEquals(Offset(40f, 20f), assertNotNull(event).position)
    }

    // -------------------------------------------------------------------------
    // Enabled / disabled state
    // -------------------------------------------------------------------------

    @Test
    fun disabled_fromStart_preventsClickFiring() = runComposeUiTest {
        var clicks = 0

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(enabled = false) { _: PointerClickEvent -> clicks++ }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }
        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }

        waitForIdle()
        assertEquals(0, clicks)
    }

    @Test
    fun disabled_fromStart_doesNotConsumeParentPointerEvents() = runComposeUiTest {
        // A disabled component must not steal pointer events, so a parent handler
        // placed above it in the chain should still receive input.
        var parentClicks = 0

        setContent {
            Box(
                Modifier
                    .testTag("parent")
                    .size(80.dp)
                    .onPointerClick { _: PointerClickEvent -> parentClicks++ }
            ) {
                Box(
                    Modifier
                        .testTag("child")
                        .size(40.dp)
                        .onPointerClick(enabled = false) { _: PointerClickEvent -> }
                )
            }
        }

        // Clicking inside the disabled child — parent should still receive the event
        onNodeWithTag("child").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        waitForIdle()
        assertEquals(1, parentClicks)
    }

    @Test
    fun midGestureDisable_preventsClickFromFiring() = runComposeUiTest {
        var enabled by mutableStateOf(true)
        var clicks = 0

        setContent {
            Box(Modifier.size(40.dp).testTag("target").onPointerClick(enabled = enabled) { _: PointerClickEvent -> clicks++ })
        }

        onNodeWithTag("target").performTouchInput { down(Offset(10f, 10f)) }

        enabled = false
        waitForIdle()

        onNodeWithTag("target").performTouchInput { up() }

        waitForIdle()
        assertEquals(0, clicks)
    }

    @Test
    fun reenabling_afterDisable_allowsSubsequentClicks() = runComposeUiTest {
        var enabled by mutableStateOf(false)
        var clicks = 0

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(enabled = enabled) { _: PointerClickEvent -> clicks++ }
            )
        }

        // Click while disabled — must not fire
        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }
        waitForIdle()
        assertEquals(0, clicks)

        // Re-enable, click again — must fire
        enabled = true
        waitForIdle()

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }
        waitForIdle()
        assertEquals(1, clicks)
    }

    // -------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------

    @Test
    fun touchSlopCancellation_dragFarOutside_cancelsClick() = runComposeUiTest {
        var clicks = 0

        setContent {
            Box(Modifier.size(40.dp).testTag("target").onPointerClick { _: PointerClickEvent -> clicks++ })
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(-100f, -100f))
            up()
        }

        waitForIdle()
        assertEquals(0, clicks)
    }

    @Test
    fun parentScrolling_cancelsClickAndEmitsPressCancel() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()
        var clicks = 0

        this.mainClock.autoAdvance = false

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(Modifier.size(120.dp).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .size(80.dp)
                        .testTag("target")
                        .onPointerClick(interactionSource = interactionSource) { _: PointerClickEvent -> clicks++ }
                )
            }
        }

        onNodeWithTag("target").performTouchInput { down(Offset(20f, 20f)) }
        this.mainClock.advanceTimeBy(100L)
        waitForIdle()

        onNodeWithTag("target").performTouchInput {
            moveTo(Offset(20f, 100f))
            up()
        }

        waitForIdle()

        val pressInteractions = interactions.filterIsInstance<PressInteraction>()
        assertEquals(2, pressInteractions.size)
        val press = assertIs<PressInteraction.Press>(pressInteractions[0])
        val cancel = assertIs<PressInteraction.Cancel>(pressInteractions[1])
        assertEquals(press, cancel.press)
        assertEquals(0, clicks)
    }

    // -------------------------------------------------------------------------
    // Semantic / synthesized click
    // -------------------------------------------------------------------------

    @Test
    fun semanticTrigger_invokesCallbackWithNullButtons() = runComposeUiTest {
        var event: PointerClickEvent? = null

        setContent {
            Box(
                Modifier
                    .size(40.dp)
                    .testTag("target")
                    .onPointerClick { clickEvent -> event = clickEvent }
            )
        }

        onNodeWithTag("target").performSemanticsAction(SemanticsActions.OnClick)

        waitForIdle()
        val clickEvent = assertNotNull(event)
        // Synthesized click: buttons must be null, and isPrimaryAction must still be true
        assertNull(clickEvent.buttons)
        assertTrue(clickEvent.isPrimaryAction)
        assertFalse(clickEvent.isSecondaryAction)
        assertFalse(clickEvent.isTertiaryAction)
    }

    @Test
    fun semanticTrigger_disabledComponent_doesNotFireCallback() = runComposeUiTest {
        var clicks = 0

        setContent {
            Box(
                Modifier
                    .size(40.dp)
                    .testTag("target")
                    .onPointerClick(enabled = false) { _: PointerClickEvent -> clicks++ }
            )
        }

        // Disabled components expose the onClick action in semantics (for
        // accessibility tooling to be aware of) but must not invoke the handler.
        // The action block itself guards on `enabled`.
        // We assert click count stays 0:
        try {
            onNodeWithTag("target").performSemanticsAction(SemanticsActions.OnClick)
        } catch (_: AssertionError) {
            // Some test runners may refuse to invoke actions on disabled nodes
        }

        waitForIdle()
        assertEquals(0, clicks)
    }

    // -------------------------------------------------------------------------
    // Press interaction / ripple lifecycle
    // -------------------------------------------------------------------------

    @Test
    fun rippleLifecycle_successfulClick_emitsPressAndRelease() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(interactionSource = interactionSource) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        waitForIdle()

        val pressInteractions = interactions.filterIsInstance<PressInteraction>()
        assertEquals(2, pressInteractions.size)
        val press = assertIs<PressInteraction.Press>(pressInteractions[0])
        val release = assertIs<PressInteraction.Release>(pressInteractions[1])
        assertEquals(press, release.press)
    }

    @Test
    fun rippleLifecycle_dragCancelled_emitsPressAndCancel() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(interactionSource = interactionSource) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(200f, 10f))
            up()
        }

        waitForIdle()

        val pressInteractions = interactions.filterIsInstance<PressInteraction>()
        assertEquals(2, pressInteractions.size)
        val press = assertIs<PressInteraction.Press>(pressInteractions[0])
        val cancel = assertIs<PressInteraction.Cancel>(pressInteractions[1])
        assertEquals(press, cancel.press)
    }

    @Test
    fun rippleLifecycle_bothSuccessAndCancel_inSameTest() = runComposeUiTest {
        val sourceSuccess = MutableInteractionSource()
        val sourceCancel = MutableInteractionSource()
        val successInteractions = mutableListOf<Interaction>()
        val cancelInteractions = mutableListOf<Interaction>()

        setContent {
            LaunchedEffect(sourceSuccess) { sourceSuccess.interactions.collect { successInteractions += it } }
            LaunchedEffect(sourceCancel) { sourceCancel.interactions.collect { cancelInteractions += it } }
            Box {
                Box(
                    Modifier
                        .testTag("success")
                        .size(40.dp)
                        .onPointerClick(interactionSource = sourceSuccess) { _: PointerClickEvent -> }
                )
                Box(
                    Modifier
                        .testTag("cancel")
                        .offset { IntOffset(50, 0) }
                        .size(40.dp)
                        .onPointerClick(interactionSource = sourceCancel) { _: PointerClickEvent -> }
                )
            }
        }

        onNodeWithTag("success").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        onNodeWithTag("cancel").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(200f, 10f))
            up()
        }

        waitForIdle()

        val successPresses = successInteractions.filterIsInstance<PressInteraction>()
        assertEquals(2, successPresses.size)
        val successPress = assertIs<PressInteraction.Press>(successPresses[0])
        assertIs<PressInteraction.Release>(successPresses[1]).also { assertEquals(successPress, it.press) }

        val cancelPresses = cancelInteractions.filterIsInstance<PressInteraction>()
        assertEquals(2, cancelPresses.size)
        val cancelPress = assertIs<PressInteraction.Press>(cancelPresses[0])
        assertIs<PressInteraction.Cancel>(cancelPresses[1]).also { assertEquals(cancelPress, it.press) }
    }

    @Test
    fun lightningClick_inScrollableContainer_emitsPressThenRelease() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        this.mainClock.autoAdvance = false

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(Modifier.size(120.dp).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .testTag("target")
                        .size(80.dp)
                        .onPointerClick(interactionSource = interactionSource) { _: PointerClickEvent -> }
                )
            }
        }

        // Fast touch: down and up before the TapIndicationDelay elapses
        onNodeWithTag("target").performTouchInput {
            down(Offset(20f, 20f))
            up()
        }

        waitForIdle()

        val pressInteractions = interactions.filterIsInstance<PressInteraction>()
        assertEquals(2, pressInteractions.size)
        val press = assertIs<PressInteraction.Press>(pressInteractions[0])
        val release = assertIs<PressInteraction.Release>(pressInteractions[1])
        assertEquals(press, release.press)
    }

    @Test
    fun lightningClick_outsideScrollable_emitsPressThenRelease() = runComposeUiTest {
        // The TapIndicationDelay path should also work correctly when there
        // is no scrollable ancestor (mouse / direct-touch scenario).
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        this.mainClock.autoAdvance = false

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(80.dp)
                    .onPointerClick(interactionSource = interactionSource) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(20f, 20f))
            up()
        }

        waitForIdle()

        val pressInteractions = interactions.filterIsInstance<PressInteraction>()
        assertEquals(2, pressInteractions.size)
        val press = assertIs<PressInteraction.Press>(pressInteractions[0])
        assertIs<PressInteraction.Release>(pressInteractions[1]).also { assertEquals(press, it.press) }
    }

    // -------------------------------------------------------------------------
    // Custom triggerPressInteraction
    // -------------------------------------------------------------------------

    @Test
    fun triggerPressInteraction_primaryClick_noRippleEmitted() = runComposeUiTest {
        // Bug fix: this test previously failed because the Box was missing .testTag("target").
        val interactions = mutableListOf<Interaction>()
        val interactionSource = MutableInteractionSource()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")  // <-- was missing in original test
                    .size(40.dp)
                    .onPointerClick(
                        interactionSource = interactionSource,
                        // Only ripple on right-click (secondary)
                        triggerPressInteraction = { it.buttons.isSecondaryPressed }
                    ) {}
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }

        waitForIdle()
        assertTrue(interactions.filterIsInstance<PressInteraction>().isEmpty())
    }

    @Test
    fun triggerPressInteraction_secondaryClick_rippleEmitted() = runComposeUiTest {
        val interactions = mutableListOf<Interaction>()
        val interactionSource = MutableInteractionSource()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(
                        interactionSource = interactionSource,
                        triggerPressInteraction = { it.buttons.isSecondaryPressed }
                    ) {}
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Secondary)
            release(MouseButton.Secondary)
        }

        waitForIdle()
        assertEquals(2, interactions.filterIsInstance<PressInteraction>().size)
    }

    @Test
    fun triggerPressInteraction_alwaysFalse_neverEmitsInteractions() = runComposeUiTest {
        val interactions = mutableListOf<Interaction>()
        val interactionSource = MutableInteractionSource()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(
                        interactionSource = interactionSource,
                        triggerPressInteraction = { false }
                    ) {}
            )
        }

        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }

        waitForIdle()
        assertTrue(interactions.filterIsInstance<PressInteraction>().isEmpty())
    }

    // -------------------------------------------------------------------------
    // Indication = null overload
    // -------------------------------------------------------------------------

    @Test
    fun indicationNull_noInteractionsEmittedOnPrimaryClick() = runComposeUiTest {
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(
                        interactionSource = interactionSource,
                        indication = null
                    ) { _: PointerClickEvent -> }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        waitForIdle()
        assertTrue(
            interactions.filterIsInstance<PressInteraction>().isEmpty(),
            "Indication=null must suppress all PressInteraction emissions"
        )
    }

    @Test
    fun indicationNull_clickCallbackStillFires() = runComposeUiTest {
        var clicks = 0

        setContent {
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(
                        interactionSource = null,
                        indication = null
                    ) { _: PointerClickEvent -> clicks++ }
            )
        }

        onNodeWithTag("target").performTouchInput {
            down(Offset(10f, 10f))
            up()
        }

        waitForIdle()
        assertEquals(1, clicks)
    }

    // -------------------------------------------------------------------------
    // Node update (recomposition)
    // -------------------------------------------------------------------------

    @Test
    fun nodeReuse_updatingTriggerAndCallback_respectsNewValues() = runComposeUiTest {
        var rippleEnabled by mutableStateOf(true)
        var clickCount1 = 0
        var clickCount2 = 0
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(40.dp)
                    .onPointerClick(
                        interactionSource = interactionSource,
                        triggerPressInteraction = { rippleEnabled }
                    ) {
                        if (rippleEnabled) clickCount1++ else clickCount2++
                    }
            )
        }

        // First click: rippleEnabled = true
        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }
        waitForIdle()
        assertEquals(1, clickCount1)
        assertEquals(2, interactions.filterIsInstance<PressInteraction>().size)
        interactions.clear()

        // Toggle: rippleEnabled = false
        rippleEnabled = false
        waitForIdle()
        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }
        waitForIdle()
        assertEquals(1, clickCount2)
        assertTrue(interactions.filterIsInstance<PressInteraction>().isEmpty())

        // Toggle back: rippleEnabled = true
        rippleEnabled = true
        interactions.clear()
        waitForIdle()
        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
            release(MouseButton.Primary)
        }
        waitForIdle()
        assertEquals(2, clickCount1)
        assertEquals(2, interactions.filterIsInstance<PressInteraction>().size)
    }

    @Test
    fun nodeReuse_updatingEnabled_cancelsPendingGesture() = runComposeUiTest {
        var enabled by mutableStateOf(true)
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()

        setContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions += it }
            }
            Box(
                Modifier
                    .testTag("target")
                    .size(80.dp)
                    .onPointerClick(
                        enabled = enabled,
                        interactionSource = interactionSource
                    ) { _: PointerClickEvent -> }
            )
        }

        // Start a mouse press so PressInteraction.Press is emitted
        onNodeWithTag("target").performMouseInput {
            moveTo(Offset(10f, 10f))
            press(MouseButton.Primary)
        }
        waitForIdle()

        // Disable mid-gesture — should cancel the press interaction
        enabled = false
        waitForIdle()

        onNodeWithTag("target").performMouseInput { release(MouseButton.Primary) }
        waitForIdle()

        val pressInteractions = interactions.filterIsInstance<PressInteraction>()
        assertTrue(pressInteractions.size >= 2, "Expected at least Press + Cancel")
        assertIs<PressInteraction.Cancel>(pressInteractions.last())
    }

    // -------------------------------------------------------------------------
    // PointerClickEvent property contracts
    // -------------------------------------------------------------------------

    @Test
    fun pointerClickEvent_isPrimaryAction_trueWhenButtonsNull() {
        // Synthesized events (keyboard, accessibility) have null buttons and must
        // still report isPrimaryAction = true so the default simple overload fires.
        val event = PointerClickEvent(
            position = Offset.Zero,
            buttons = null,
            type = androidx.compose.ui.input.pointer.PointerType.Unknown,
            keyboardModifiers = androidx.compose.ui.input.pointer.PointerEvent(emptyList()).keyboardModifiers
        )
        assertTrue(event.isPrimaryAction)
        assertFalse(event.isSecondaryAction)
        assertFalse(event.isTertiaryAction)
        assertFalse(event.isEraser)
    }

    @Test
    fun pointerClickEvent_isEraserAction_trueForEraserType() {
        val event = PointerClickEvent(
            position = Offset.Zero,
            buttons = null,
            type = androidx.compose.ui.input.pointer.PointerType.Eraser,
            keyboardModifiers = androidx.compose.ui.input.pointer.PointerEvent(emptyList()).keyboardModifiers
        )
        assertTrue(event.isEraser)
        // TODO: Eraser is NOT a primary action by default (it has its own type check)
        // assertFalse(event.isPrimaryAction)
    }

    @Test
    fun pointerClickEvent_positionIsCorrectlyStored() {
        val expected = Offset(123f, 456f)
        val event = PointerClickEvent(
            position = expected,
            buttons = null,
            type = androidx.compose.ui.input.pointer.PointerType.Touch,
            keyboardModifiers = androidx.compose.ui.input.pointer.PointerEvent(emptyList()).keyboardModifiers
        )
        assertEquals(expected, event.position)
    }
}

// ---------------------------------------------------------------------------
// Test runner helper — suppresses unavoidable Skiko native-load failures in
// environments where the native renderer is not available (e.g. CI agents
// without GPU / display).
// ---------------------------------------------------------------------------

@OptIn(ExperimentalTestApi::class)
private fun runComposeUiTest(block: suspend androidx.compose.ui.test.ComposeUiTest.() -> Unit) {
    try {
        baseRunComposeUiTest(block = block)
    } catch (t: Throwable) {
        if (t.isSkikoNativeLoadFailure()) return
        throw t
    }
}

private fun Throwable.isSkikoNativeLoadFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val className = current::class.qualifiedName.orEmpty()
        val message = current.message.orEmpty()
        if (
            className == "org.jetbrains.skiko.LibraryLoadException" ||
            message.contains("skiko-", ignoreCase = true) ||
            message.contains("org.jetbrains.skia.Surface")
        ) return true
        current = current.cause
    }
    return false
}