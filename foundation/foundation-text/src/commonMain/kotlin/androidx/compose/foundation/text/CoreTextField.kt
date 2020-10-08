/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("DEPRECATION_ERROR")

package androidx.compose.foundation.text

import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.foundation.text.selection.SelectionHandle
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.invalidate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.state
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.drawBehind
import androidx.compose.ui.drawWithContent
import androidx.compose.ui.focus
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.compose.ui.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.longPressDragGestureFilter
import androidx.compose.ui.gesture.pressIndicatorGestureFilter
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.ClipboardManagerAmbient
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.FontLoaderAmbient
import androidx.compose.ui.platform.HapticFeedBackAmbient
import androidx.compose.ui.platform.TextInputServiceAmbient
import androidx.compose.ui.platform.TextToolbarAmbient
import androidx.compose.ui.selection.SelectionLayout
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SoftwareKeyboardController
import androidx.compose.ui.text.TextDelegate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.NO_SESSION
import androidx.compose.ui.text.input.OffsetMap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.annotation.VisibleForTesting
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Base composable that enables users to edit text via hardware or software keyboard.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well
 * as selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [CoreTextField] in
 * order to have the final state of the text being displayed. Example usage:
 * @sample androidx.compose.foundation.text.samples.CoreTextFieldSample
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * @param value The [androidx.compose.ui.text.input.TextFieldValue] to be shown in the [CoreTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardType The keyboard type to be used in this text field. Note that this input type
 * is honored by IME and shows corresponding keyboard but this is not guaranteed. For example,
 * some IME may send non-ASCII character even if you set [KeyboardType.Ascii].
 * @param imeAction The IME action. This IME action is honored by IME and may show specific icons
 * on the keyboard. For example, search icon may be shown if [ImeAction.Search] is specified.
 * Then, when user tap that key, the [onImeActionPerformed] callback is called with specified
 * ImeAction.
 * @param onImeActionPerformed Called when the input service requested an IME action. When the
 * input service emitted an IME action, this callback is called with the emitted IME action. Note
 * that this IME action may be different from what you specified in [imeAction].
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated.
 * @param onTextInputStarted Callback that is executed when the initialization has done for
 * communicating with platform text input service, e.g. software keyboard on Android. Called with
 * [SoftwareKeyboardController] instance which can be used for requesting input show/hide software
 * keyboard.
 * @param cursorColor Color of the cursor. If [Color.Unspecified], there will be no cursor drawn
 */
@Composable
@OptIn(
    ExperimentalFocus::class,
    InternalTextApi::class
)
fun CoreTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle = TextStyle.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onImeActionPerformed: (ImeAction) -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    cursorColor: Color = Color.Unspecified
) {
    // If developer doesn't pass new value to TextField, recompose won't happen but internal state
    // and IME may think it is updated. To fix this inconsistent state, enforce recompose.
    val recompose = invalidate
    val focusRequester = FocusRequester()

    // Ambients
    val textInputService = TextInputServiceAmbient.current
    val density = DensityAmbient.current
    val resourceLoader = FontLoaderAmbient.current

    // State
    val (visualText, offsetMap) = remember(value, visualTransformation) {
        val transformed = visualTransformation.filter(AnnotatedString(value.text))
        value.composition?.let {
            TextFieldDelegate.applyCompositionDecoration(it, transformed)
        } ?: transformed
    }
    val state = remember {
        TextFieldState(
            TextDelegate(
                text = visualText,
                style = textStyle,
                density = density,
                resourceLoader = resourceLoader
            )
        )
    }
    state.update(
        visualText,
        textStyle,
        density,
        resourceLoader,
        onValueChange,
        onImeActionPerformed
    )

    val onValueChangeWrapper: (TextFieldValue) -> Unit = {
        state.onValueChange(it)
        recompose()
    }
    val onImeActionPerformedWrapper: (ImeAction) -> Unit = {
        state.onImeActionPerformed(it)
    }

    state.processor.onNewState(value, textInputService, state.inputSession)

    val manager = remember { TextFieldSelectionManager() }
    manager.offsetMap = offsetMap
    manager.onValueChange = onValueChangeWrapper
    manager.state = state
    manager.value = value
    manager.clipboardManager = ClipboardManagerAmbient.current
    manager.textToolbar = TextToolbarAmbient.current
    manager.hapticFeedBack = HapticFeedBackAmbient.current

    val focusObserver = Modifier.focusObserver {
        if (state.hasFocus == it.isFocused) {
            return@focusObserver
        }

        state.hasFocus = it.isFocused

        if (it.isFocused) {
            state.inputSession = TextFieldDelegate.onFocus(
                textInputService,
                value,
                state.processor,
                keyboardType,
                imeAction,
                onValueChangeWrapper,
                onImeActionPerformedWrapper
            )
            if (state.inputSession != NO_SESSION && textInputService != null) {
                onTextInputStarted(
                    SoftwareKeyboardController(
                        textInputService,
                        state.inputSession
                    )
                )
            }
            state.layoutCoordinates?.let { coords ->
                textInputService?.let { textInputService ->
                    state.layoutResult?.let { layoutResult ->
                        TextFieldDelegate.notifyFocusedRect(
                            value,
                            state.textDelegate,
                            layoutResult,
                            coords,
                            textInputService,
                            state.inputSession,
                            state.hasFocus,
                            offsetMap
                        )
                    }
                }
            }
        } else {
            TextFieldDelegate.onBlur(
                textInputService,
                state.inputSession,
                state.processor,
                false,
                onValueChangeWrapper
            )
            manager.deselect()
        }
    }

    val focusRequestTapModifier = Modifier.tapGestureFilter {
        if (!state.hasFocus) {
            focusRequester.requestFocus()
        } else {
            // if already focused make sure tap request keyboard.
            textInputService?.showSoftwareKeyboard(state.inputSession)
        }
    }

    val dragPositionGestureModifier = Modifier.dragPositionGestureFilter(
        onPress = {
            if (state.hasFocus) {
                state.selectionIsOn = false
                manager.hideSelectionToolbar()
            }
        },
        onRelease = {
            if (state.hasFocus && !state.selectionIsOn) {
                state.layoutResult?.let { layoutResult ->
                    TextFieldDelegate.setCursorOffset(
                        it,
                        layoutResult,
                        state.processor,
                        offsetMap,
                        onValueChangeWrapper
                    )
                }
            }
        }
    )

    val selectionLongPressModifier = Modifier.longPressDragGestureFilter(
        manager.longPressDragObserver
    )

    val drawModifier = Modifier.drawBehind {
        state.layoutResult?.let { layoutResult ->
            drawIntoCanvas { canvas ->
                TextFieldDelegate.draw(
                    canvas,
                    value,
                    offsetMap,
                    layoutResult,
                    DefaultSelectionColor
                )
            }
        }
    }

    val onPositionedModifier = Modifier.onGloballyPositioned {
        if (textInputService != null) {
            state.layoutCoordinates = it
            if (state.selectionIsOn) {
                if (state.showFloatingToolbar) manager.showSelectionToolbar()
                else manager.hideSelectionToolbar()
            }
            state.layoutResult?.let { layoutResult ->
                TextFieldDelegate.notifyFocusedRect(
                    value,
                    state.textDelegate,
                    layoutResult,
                    it,
                    textInputService,
                    state.inputSession,
                    state.hasFocus,
                    offsetMap
                )
            }
        }
    }

    val semanticsModifier = Modifier.semantics {
        this.imeAction = imeAction
        this.supportsInputMethods()
        this.text = AnnotatedString(value.text)
        this.textSelectionRange = value.selection
        this.focused = state.hasFocus
        getTextLayoutResult {
            if (state.layoutResult != null) {
                it.add(state.layoutResult!!)
                true
            } else {
                false
            }
        }
        setText {
            onValueChangeWrapper(TextFieldValue(it.text, TextRange(it.text.length)))
            true
        }
        setSelection { start, end, traversalMode ->
            if (start == value.selection.start && end == value.selection.end) {
                false
            } else if (start.coerceAtMost(end) >= 0 &&
                start.coerceAtLeast(end) <= value.text.length
            ) {
                // Do not show toolbar if it's a traversal mode (with the volume keys), or
                // if the cursor just moved to beginning or end.
                if (traversalMode || start == end) {
                    manager.exitSelectionMode()
                } else {
                    manager.enterSelectionMode()
                    if (!state.hasFocus) focusRequester.requestFocus()
                }
                onValueChangeWrapper(TextFieldValue(value.text, TextRange(start, end)))
                true
            } else {
                manager.exitSelectionMode()
                false
            }
        }
        onClick {
            if (!state.hasFocus) {
                focusRequester.requestFocus()
            } else {
                textInputService?.showSoftwareKeyboard(state.inputSession)
            }
            true
        }
        onLongClick {
            manager.enterSelectionMode()
            if (!state.hasFocus) focusRequester.requestFocus()
            true
        }
        if (!value.selection.collapsed) {
            copyText {
                manager.copy()
                true
            }
            cutText {
                manager.cut()
                true
            }
        }
        pasteText {
            manager.paste()
            true
        }
    }

    val cursorModifier =
        Modifier.cursor(state, value, offsetMap, cursorColor)

    onDispose { manager.hideSelectionToolbar() }

    val modifiers = modifier.focusRequester(focusRequester)
        .then(focusObserver)
        .then(cursorModifier)
        .then(dragPositionGestureModifier)
        .then(selectionLongPressModifier)
        .then(focusRequestTapModifier)
        .then(drawModifier)
        .then(onPositionedModifier)
        .then(semanticsModifier)
        .focus()

    SelectionLayout(modifiers) {
        Layout(emptyContent()) { _, constraints ->
            TextFieldDelegate.layout(
                state.textDelegate,
                constraints,
                layoutDirection,
                state.layoutResult
            ).let { (width, height, result) ->
                if (state.layoutResult != result) {
                    state.layoutResult = result
                    onTextLayout(result)
                }
                layout(
                    width,
                    height,
                    mapOf(
                        FirstBaseline to result.firstBaseline.roundToInt(),
                        LastBaseline to result.lastBaseline.roundToInt()
                    )
                ) {}
            }
        }

        if (state.hasFocus && state.selectionIsOn) {
            manager.state?.layoutResult?.let {
                if (!value.selection.collapsed) {
                    val startDirection = it.getBidiRunDirection(value.selection.start)
                    val endDirection =
                        it.getBidiRunDirection(max(value.selection.end - 1, 0))
                    val directions = Pair(startDirection, endDirection)
                    SelectionHandle(
                        isStartHandle = true,
                        directions = directions,
                        manager = manager
                    )
                    SelectionHandle(
                        isStartHandle = false,
                        directions = directions,
                        manager = manager
                    )
                }

                manager.state?.let {
                    // If in selection mode (when the floating toolbar is shown) a new symbol
                    // from the keyboard is entered, text field should enter the editing mode
                    // instead.
                    if (manager.isTextChanged()) it.showFloatingToolbar = false
                    if (it.hasFocus) {
                        if (it.showFloatingToolbar) manager.showSelectionToolbar()
                        else manager.hideSelectionToolbar()
                    }
                }
            }
        } else manager.hideSelectionToolbar()
    }
}

@OptIn(InternalTextApi::class)
internal class TextFieldState(
    var textDelegate: TextDelegate
) {
    val processor = EditProcessor()
    var inputSession = NO_SESSION

    /**
     * This should be a state as every time we update the value we need to redraw it.
     * state observation during onDraw callback will make it work.
     */
    var hasFocus by mutableStateOf(false)

    /** The last layout coordinates for the Text's layout, used by selection */
    var layoutCoordinates: LayoutCoordinates? = null

    /** The latest TextLayoutResult calculated in the measure block */
    var layoutResult: TextLayoutResult? = null

    /**
     * The gesture detector status, to indicate whether current status is selection or editing.
     *
     * In the editing mode, there is no selection shown, only cursor is shown. To enter the editing
     * mode from selection mode, just tap on the screen.
     *
     * In the selection mode, there is no cursor shown, only selection is shown. To enter
     * the selection mode, just long press on the screen. In this mode, finger movement on the
     * screen changes selection instead of moving the cursor.
     */
    var selectionIsOn by mutableStateOf(false)

    /**
     * A flag to check if the selection start or end handle is being dragged.
     * If this value is true, then onPress will not select any text.
     * This value will be set to true when either handle is being dragged, and be reset to false
     * when the dragging is stopped.
     */
    var draggingHandle = false

    /**
     * A flag to check if the floating toolbar should show.
     */
    var showFloatingToolbar = false

    var onImeActionPerformed: (ImeAction) -> Unit = {}
        private set

    var onValueChange: (TextFieldValue) -> Unit = {}
        private set

    fun update(
        visualText: AnnotatedString,
        textStyle: TextStyle,
        density: Density,
        resourceLoader: Font.ResourceLoader,
        onValueChange: (TextFieldValue) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        this.onValueChange = onValueChange
        this.onImeActionPerformed = onImeActionPerformed

        textDelegate = updateTextDelegate(
            current = textDelegate,
            text = visualText,
            style = textStyle,
            density = density,
            resourceLoader = resourceLoader,
            placeholders = emptyList()
        )
    }
}

/**
 * Helper class for tracking dragging event.
 */
internal class DragEventTracker {
    private var origin = Offset.Zero
    private var distance = Offset.Zero

    /**
     * Restart the tracking from given origin.
     *
     * @param origin The origin of the drag gesture.
     */
    fun init(origin: Offset) {
        this.origin = origin
    }

    /**
     * Pass distance parameter called by DragGestureDetector$onDrag callback
     *
     * @param distance The distance from the origin of the drag origin.
     */
    fun onDrag(distance: Offset) {
        this.distance = distance
    }

    /**
     * Returns the current position.
     *
     * @return The position of the current drag point.
     */
    fun getPosition(): Offset {
        return origin + distance
    }
}

/**
 * Helper composable for tracking drag position.
 */
@Composable
private fun Modifier.dragPositionGestureFilter(
    onPress: (Offset) -> Unit,
    onRelease: (Offset) -> Unit
): Modifier {
    val tracker = remember { DragEventTracker() }
    // TODO(shepshapard): PressIndicator doesn't seem to be the right thing to use here.  It
    //  actually may be functionally correct, but might mostly suggest that it should not
    //  actually be called PressIndicator, but instead something else.

    return this
        .pressIndicatorGestureFilter(
            onStart = {
                tracker.init(it)
                onPress(it)
            },
            onStop = {
                onRelease(tracker.getPosition())
            }
        )
        .dragGestureFilter(
            dragObserver = object :
                DragObserver {
                override fun onDrag(dragDistance: Offset): Offset {
                    tracker.onDrag(dragDistance)
                    return Offset.Zero
                }
            }
        )
}

private val cursorAnimationSpec: AnimationSpec<Float>
    get() = repeatable(
        iterations = AnimationConstants.Infinite,
        animation = keyframes {
            durationMillis = 1000
            1f at 0
            1f at 499
            0f at 500
            0f at 999
        }
    )

private val DefaultCursorThickness = 2.dp

@OptIn(InternalTextApi::class)
private fun Modifier.cursor(
    state: TextFieldState,
    value: TextFieldValue,
    offsetMap: OffsetMap,
    cursorColor: Color
) = composed {
    // this should be a disposable clock, but it's not available in this module
    // however, we only launch one animation and guarantee that we stop it (via snap) in dispose
    val animationClocks = AnimationClockAmbient.current
    val cursorAlpha = remember(animationClocks) { AnimatedFloatModel(0f, animationClocks) }

    if (state.hasFocus && value.selection.collapsed && cursorColor != Color.Unspecified) {
        onCommit(cursorColor, value.text) {
            if (blinkingCursorEnabled) {
                cursorAlpha.animateTo(0f, anim = cursorAnimationSpec)
            } else {
                cursorAlpha.snapTo(1f)
            }
            onDispose {
                cursorAlpha.snapTo(0f)
            }
        }
        drawWithContent {
            this.drawContent()
            val cursorAlphaValue = cursorAlpha.value.coerceIn(0f, 1f)
            if (cursorAlphaValue != 0f) {
                val transformedOffset = offsetMap
                    .originalToTransformed(value.selection.start)
                val cursorRect = state.layoutResult?.getCursorRect(transformedOffset)
                    ?: Rect(0f, 0f, 0f, 0f)
                val cursorWidth = DefaultCursorThickness.toPx()
                val cursorX = (cursorRect.left + cursorWidth / 2)
                    .coerceAtMost(size.width - cursorWidth / 2)

                drawLine(
                    cursorColor,
                    Offset(cursorX, cursorRect.top),
                    Offset(cursorX, cursorRect.bottom),
                    alpha = cursorAlphaValue,
                    strokeWidth = cursorWidth
                )
            }
        }
    } else {
        Modifier
    }
}

@Stable
private class AnimatedFloatModel(
    initialValue: Float,
    clock: AnimationClockObservable,
    visibilityThreshold: Float = Spring.DefaultDisplacementThreshold
) : AnimatedFloat(clock, visibilityThreshold) {
    override var value: Float by mutableStateOf(initialValue, structuralEqualityPolicy())
}

// TODO(b/151940543): Remove this variable when we have a solution for idling animations
@InternalTextApi
    /** @suppress */
var blinkingCursorEnabled: Boolean = true
    @VisibleForTesting
    set