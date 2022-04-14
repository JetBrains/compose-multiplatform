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

// TODO(b/160821157): Replace FocusState with FocusState2.isFocused
@file:Suppress("DEPRECATION")

package androidx.compose.foundation.textfield

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.computeSizeForDefaultText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.test.R
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextFieldValue.Companion.Saver
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private val Tag = "textField"

    // This sample font provides the following features:
    // 1. The width of most of visible characters equals to font size.
    // 2. The LTR/RTL characters are rendered as ▶/◀.
    // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
    private val measureFontFamily = Font(
        resId = R.font.sample_font,
        weight = FontWeight.Normal,
        style = FontStyle.Normal
    ).toFontFamily()

    @Test
    fun textField_focusInSemantics() {
        val inputService = TextInputService(mock())

        var isFocused = false
        rule.setContent {
            val state = remember { mutableStateOf("") }
            CompositionLocalProvider(
                LocalTextInputService provides inputService
            ) {
                BasicTextField(
                    value = state.value,
                    modifier = Modifier.fillMaxSize().onFocusChanged { isFocused = it.isFocused },
                    onValueChange = { state.value = it }
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()

        rule.runOnIdle {
            assertThat(isFocused).isTrue()
        }
    }

    @Composable
    private fun TextFieldApp() {
        val state = remember { mutableStateOf("") }
        BasicTextField(
            value = state.value,
            modifier = Modifier.fillMaxSize(),
            onValueChange = {
                state.value = it
            }
        )
    }

    @Test
    fun textField_commitTexts() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)

        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                TextFieldApp()
            }
        }

        rule.onNode(hasSetTextAction()).performClick()

        var onEditCommandCallback: ((List<EditCommand>) -> Unit)? = null
        rule.runOnIdle {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditCommand>) -> Unit>()
            verify(platformTextInputService, times(1)).startInput(
                value = any(),
                imeOptions = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        arrayOf(
            listOf(CommitTextCommand("1", 1)),
            listOf(CommitTextCommand("a", 1)),
            listOf(CommitTextCommand("2", 1)),
            listOf(CommitTextCommand("b", 1)),
            listOf(CommitTextCommand("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            rule.runOnIdle { onEditCommandCallback!!.invoke(it) }
        }

        rule.runOnIdle {
            val stateCaptor = argumentCaptor<TextFieldValue>()
            verify(platformTextInputService, atLeastOnce())
                .updateState(any(), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "1a2b3".
            assertThat(stateCaptor.lastValue.text).isEqualTo("1a2b3")
        }
    }

    @Composable
    private fun OnlyDigitsApp() {
        val state = remember { mutableStateOf("") }
        BasicTextField(
            value = state.value,
            modifier = Modifier.fillMaxSize(),
            onValueChange = { value ->
                if (value.all { it.isDigit() }) {
                    state.value = value
                }
            }
        )
    }

    @Test
    fun textField_commitTexts_state_may_not_set() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)

        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                OnlyDigitsApp()
            }
        }

        rule.onNode(hasSetTextAction()).performClick()

        var onEditCommandCallback: ((List<EditCommand>) -> Unit)? = null
        rule.runOnIdle {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditCommand>) -> Unit>()
            verify(platformTextInputService, times(1)).startInput(
                value = any(),
                imeOptions = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "a", "2", "b", "3". Only numbers should remain.
        arrayOf(
            listOf(CommitTextCommand("1", 1)),
            listOf(CommitTextCommand("a", 1)),
            listOf(CommitTextCommand("2", 1)),
            listOf(CommitTextCommand("b", 1)),
            listOf(CommitTextCommand("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            rule.runOnIdle { onEditCommandCallback!!.invoke(it) }
        }

        rule.runOnIdle {
            val stateCaptor = argumentCaptor<TextFieldValue>()
            verify(platformTextInputService, atLeastOnce())
                .updateState(any(), stateCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123" since
            // the rejects if the incoming model contains alphabets.
            assertThat(stateCaptor.lastValue.text).isEqualTo("123")
        }
    }

    @Test
    fun textField_onTextLayoutCallback() {
        val platformTextInputService = mock<PlatformTextInputService>()
        val textInputService = TextInputService(platformTextInputService)

        val onTextLayout: (TextLayoutResult) -> Unit = mock()
        rule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides textInputService
            ) {
                val state = remember { mutableStateOf("") }
                BasicTextField(
                    value = state.value,
                    modifier = Modifier.fillMaxSize(),
                    onValueChange = {
                        state.value = it
                    },
                    onTextLayout = onTextLayout
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()

        var onEditCommandCallback: ((List<EditCommand>) -> Unit)? = null
        rule.runOnIdle {
            // Verify startInput is called and capture the callback.
            val onEditCommandCaptor = argumentCaptor<(List<EditCommand>) -> Unit>()
            verify(platformTextInputService, times(1)).startInput(
                value = any(),
                imeOptions = any(),
                onEditCommand = onEditCommandCaptor.capture(),
                onImeActionPerformed = any()
            )
            assertThat(onEditCommandCaptor.allValues.size).isEqualTo(1)
            onEditCommandCallback = onEditCommandCaptor.firstValue
            assertThat(onEditCommandCallback).isNotNull()
        }

        // Performs input events "1", "2", "3".
        arrayOf(
            listOf(CommitTextCommand("1", 1)),
            listOf(CommitTextCommand("2", 1)),
            listOf(CommitTextCommand("3", 1))
        ).forEach {
            // TODO: This should work only with runOnUiThread. But it seems that these events are
            // not buffered and chaining multiple of them before composition happens makes them to
            // get lost.
            rule.runOnIdle { onEditCommandCallback!!.invoke(it) }
        }

        rule.runOnIdle {
            val layoutCaptor = argumentCaptor<TextLayoutResult>()
            verify(onTextLayout, atLeastOnce()).invoke(layoutCaptor.capture())

            // Don't care about the intermediate state update. It should eventually be "123"
            assertThat(layoutCaptor.lastValue.layoutInput.text.text).isEqualTo("123")
        }
    }

    @Test
    fun textFieldInRow_fixedElementIsVisible() {
        val parentSize = 300.dp
        val boxSize = 50.dp
        var size: Int? = null
        rule.setContent {
            Box(Modifier.size(parentSize)) {
                Row {
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned {
                                size = it.size.width
                            }
                    )
                    Box(Modifier.size(boxSize))
                }
            }
        }

        with(rule.density) {
            assertThat(size).isEqualTo(parentSize.roundToPx() - boxSize.roundToPx())
        }
    }

    @Test
    fun textFieldValue_saverRestoresState() {
        var state: MutableState<TextFieldValue>? = null

        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = Saver) { mutableStateOf(TextFieldValue()) }
        }

        rule.runOnIdle {
            state!!.value = TextFieldValue("test", TextRange(1, 2))

            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(
                TextFieldValue("test", TextRange(1, 2))
            )
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun textFieldValue_saverRestoresState_withAnnotatedString() {
        var state: MutableState<TextFieldValue>? = null
        val annotatedString = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) { append("1") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("2") }
            withAnnotation(tag = "Tag1", annotation = "Annotation1") { append("3") }
            withAnnotation(VerbatimTtsAnnotation("verbatim1")) { append("4") }
            withAnnotation(tag = "Tag2", annotation = "Annotation2") { append("5") }
            withAnnotation(VerbatimTtsAnnotation("verbatim2")) { append("6") }
            withStyle(
                SpanStyle(
                    color = Color.Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.All,
                    fontFeatureSettings = "feature settings",
                    letterSpacing = 2.em,
                    baselineShift = BaselineShift.Superscript,
                    textGeometricTransform = TextGeometricTransform(2f, 3f),
                    localeList = LocaleList(
                        Locale("sr-Latn-SR"),
                        Locale("sr-Cyrl-SR"),
                        Locale.current
                    ),
                    background = Color.Blue,
                    textDecoration = TextDecoration.LineThrough,
                    shadow = Shadow(color = Color.Red, offset = Offset(2f, 2f), blurRadius = 4f)

                )
            ) {
                append("7")
            }
            withStyle(
                ParagraphStyle(
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 10.sp,
                    textIndent = TextIndent(firstLine = 2.sp, restLine = 3.sp)
                )
            ) {
                append("8")
            }
        }
        val newTextFieldValue = TextFieldValue(annotatedString, TextRange(1, 2))

        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            state = rememberSaveable(stateSaver = Saver) { mutableStateOf(TextFieldValue()) }
        }

        rule.runOnIdle {
            state!!.value = newTextFieldValue
            // we null it to ensure recomposition happened
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.value).isEqualTo(newTextFieldValue)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldNotFocused_cursorNotRendered() {
        rule.setContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier.size(10.dp, 20.dp).background(color = Color.White),
                cursorBrush = SolidColor(Color.Blue)
            )
        }

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    fun defaultSemantics() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = "",
                onValueChange = {},
                decorationBox = {
                    Column {
                        BasicText("label")
                        it()
                    }
                }
            )
        }

        rule.onNodeWithTag(Tag)
            .assertEditableTextEquals("")
            .assertTextEquals("label", includeEditableText = false)
            .assertHasClickAction()
            .assert(hasSetTextAction())
            .assert(hasImeAction(ImeAction.Default))
            .assert(isNotFocused())
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange.Zero
                )
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
        assert(textLayoutResults.size == 1) { "TextLayoutResult is null" }
    }

    @Test
    fun semantics_clickAction() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag(Tag)
            .assert(isFocused())
    }

    @Test
    fun semantics_setTextSetSelectionActions() {
        rule.setContent {
            var value by remember { mutableStateOf("") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        val hello = AnnotatedString("Hello")
        rule.onNodeWithTag(Tag)
            .assertEditableTextEquals("")
            .performSemanticsAction(SemanticsActions.SetText) { it(hello) }
            .assertEditableTextEquals(hello.text)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(hello.length)
                )
            )

        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.SetSelection) { it(1, 3, true) }
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(1, 3)
                )
            )
    }

    @Test
    fun setImeAction_isReflectedInSemantics() {
        rule.setContent {
            BasicTextField(
                value = "",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onValueChange = {}
            )
        }

        rule.onNode(hasSetTextAction())
            .assert(hasImeAction(ImeAction.Search))
    }

    @Test
    fun semantics_copyTextAction() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(0, 5)))

        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.CopyText) { it() }

        rule.runOnIdle {
            assertThat(value.selection).isEqualTo(TextRange(5, 5))
        }
    }

    @Test
    fun semantics_pasteTextAction() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(0, 6)))

        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        // copy text to the clipboard
        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.CopyText) { it() }
        rule.runOnIdle {
            assertThat(value.selection.collapsed).isTrue()
            assertThat(value.selection.start).isEqualTo(6)
        }

        // paste text from the clipboard
        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.PasteText) { it() }
        rule.runOnIdle {
            assertThat(value.text).isEqualTo("Hello Hello World")
        }
    }

    @Test
    fun semantics_cutTextAction() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(0, 6)))

        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.CutText) { it() }

        rule.runOnIdle {
            assertThat(value.text).isEqualTo("World")
            assertThat(value.selection).isEqualTo(TextRange(0, 0))
        }
    }

    @Test
    fun semantics_passwordTextField_noCopyCutActions() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = TextFieldValue("Hello", TextRange(0, 3)),
                onValueChange = {},
                visualTransformation = PasswordVisualTransformation()
            )
        }

        rule.onNodeWithTag(Tag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))
    }

    @Test
    fun semantics_transformedText() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = TextFieldValue("Hello"),
                onValueChange = {},
                visualTransformation = { text ->
                    TransformedText(
                        text.toUpperCase(LocaleList("en_US")),
                        OffsetMapping.Identity
                    )
                }
            )
        }

        rule.onNodeWithTag(Tag)
            .assertTextEquals("HELLO")
    }

    @LargeTest
    @Test
    fun semantics_longClick() {
        val text = "Hello World"
        var value by mutableStateOf(TextFieldValue(text, TextRange(text.length)))
        var toolbar: TextToolbar? = null

        rule.setContent {
            toolbar = LocalTextToolbar.current
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = value,
                onValueChange = { value = it }
            )
        }

        rule.runOnIdle {
            assertThat(toolbar?.status).isEqualTo(TextToolbarStatus.Hidden)
        }

        rule.onNodeWithTag(Tag)
            .performSemanticsAction(SemanticsActions.OnLongClick) { it() }

        rule.runOnIdle {
            assertThat(toolbar?.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun stringOverrideTextField_canDeleteLastSymbol() {
        var lastSeenText = ""
        rule.setContent {
            var text by remember { mutableStateOf("") }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    lastSeenText = it
                },
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag)
            .performTextInput("A")

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("A")
        }

        rule.onNodeWithTag(Tag)
            .performTextClearance()

        rule.runOnIdle {
            assertThat(lastSeenText).isEqualTo("")
        }
    }

    @Test
    fun decorationBox_clickable() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Column {
                BasicTextField(
                    value = "test",
                    onValueChange = {},
                    textStyle = TextStyle(fontSize = 2.sp),
                    modifier = Modifier.requiredHeight(100.dp).fillMaxWidth(),
                    decorationBox = {
                        // the core text field is at the very bottom
                        Column {
                            BasicText("Label", Modifier.testTag("label"))
                            Spacer(Modifier.weight(1f))
                            it()
                        }
                    },
                    interactionSource = interactionSource
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        // click outside core text field area
        rule.onNodeWithTag("label", useUnmergedTree = true)
            .performTouchInput {
                click(Offset.Zero)
            }

        rule.runOnIdle {
            // Not asserting total size as we have other interactions here too
            assertThat(interactions.filterIsInstance<FocusInteraction.Focus>()).hasSize(1)
        }
    }

    @Test
    fun textField_stringOverload_callsOnValueChange_whenTextChange() {
        var onValueChangeCalled = false

        rule.setContent {
            val state = remember { mutableStateOf("abc") }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = state.value,
                onValueChange = {
                    onValueChangeCalled = true
                    state.value = it
                }
            )
        }

        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(Tag)
            .performClick()
            .performTextInputSelection(TextRange(0, 0))

        // reset
        rule.runOnIdle {
            onValueChangeCalled = false
        }

        // change selection
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(Tag)
            .performTextInputSelection(TextRange(1, 1))

        rule.runOnIdle {
            assertThat(onValueChangeCalled).isFalse()
        }

        // change text
        rule.onNodeWithTag(Tag)
            .performTextInput("d")

        rule.runOnIdle {
            assertThat(onValueChangeCalled).isTrue()
        }
    }

    @Test
    @Ignore // b/184750119
    fun textField_callsOnValueChange_whenTextFieldValueChange() {
        var onValueChangeCalled = false
        var lastSeenTextFieldValue = TextFieldValue()

        rule.setContent {
            val state = remember { mutableStateOf(TextFieldValue("abc")) }
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                value = state.value,
                onValueChange = {
                    onValueChangeCalled = true
                    lastSeenTextFieldValue = it
                    state.value = it
                }
            )
        }

        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(Tag)
            .performClick()
            .performTextInputSelection(TextRange(0, 0))

        // reset flag since click might change selection
        rule.runOnIdle {
            onValueChangeCalled = false
        }

        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(Tag)
            .performTextInputSelection(TextRange(1, 1))

        // selection changed
        rule.runOnIdle {
            assertWithMessage("$lastSeenTextFieldValue").that(onValueChangeCalled).isTrue()
            // reset flag
            onValueChangeCalled = false
        }
        rule.waitUntil { onValueChangeCalled == false }

        // set selection to same value, no change should occur
        @OptIn(ExperimentalTestApi::class)
        rule.onNodeWithTag(Tag)
            .performTextInputSelection(TextRange(1, 1))

        rule.runOnIdle {
            assertWithMessage("$lastSeenTextFieldValue").that(onValueChangeCalled).isFalse()
        }

        rule.onNodeWithTag(Tag)
            .performTextInput("d")

        rule.runOnIdle {
            assertWithMessage("$lastSeenTextFieldValue").that(onValueChangeCalled).isTrue()
        }
    }

    @Test
    fun textField_stringOverload_doesNotCallOnValueChange_whenCompositionUpdatesOnly() {
        var callbackCounter = 0

        rule.setContent {
            val focusManager = LocalFocusManager.current
            val text = remember { mutableStateOf("A") }

            BasicTextField(
                value = text.value,
                onValueChange = {
                    callbackCounter += 1
                    text.value = it

                    // causes TextFieldValue's composition clearing
                    focusManager.clearFocus(true)
                },
                modifier = Modifier.testTag("tag")
            )
        }

        rule.onNodeWithTag("tag")
            .performClick()
        rule.waitForIdle()

        rule.onNodeWithTag("tag")
            .performTextClearance()

        rule.runOnIdle {
            assertThat(callbackCounter).isEqualTo(1)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_defaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle = TextStyle(
            textAlign = TextAlign.Center,
            color = Color.Black,
            fontFamily = measureFontFamily,
            fontSize = fontSize.sp
        )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag),
                    value = "H",
                    onValueChange = { },
                    textStyle = textStyle,
                    singleLine = true
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertCentered(fontSize)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_widthSmallerThanDefaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle = TextStyle(
            textAlign = TextAlign.Center,
            color = Color.Black,
            fontFamily = measureFontFamily,
            fontSize = fontSize.sp
        )
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultWidth = computeSizeForDefaultText(
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                maxLines = 1
            ).width

            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag).width(defaultWidth.dp / 2),
                    value = "H",
                    onValueChange = { },
                    textStyle = textStyle,
                    singleLine = true
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertCentered(fontSize)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textField_textAlignCenter_widthLargerThanDefaultWidth() {
        val fontSize = 50
        val density = Density(1f, 1f)
        val textStyle = TextStyle(
            textAlign = TextAlign.Center,
            color = Color.Black,
            fontFamily = measureFontFamily,
            fontSize = fontSize.sp
        )
        rule.setContent {
            val fontFamilyResolver = LocalFontFamilyResolver.current
            val defaultWidth = computeSizeForDefaultText(
                style = textStyle,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                maxLines = 1
            ).width

            CompositionLocalProvider(LocalDensity provides density) {
                BasicTextField(
                    modifier = Modifier.testTag(Tag).width(defaultWidth.dp * 2),
                    value = "H",
                    onValueChange = { },
                    textStyle = textStyle,
                    singleLine = true
                )
            }
        }

        rule.waitForIdle()
        rule.onNodeWithTag(Tag).captureToImage().assertCentered(fontSize)
    }
}

private fun SemanticsNodeInteraction.assertEditableTextEquals(
    value: String
): SemanticsNodeInteraction =
    assert(
        SemanticsMatcher("${SemanticsProperties.EditableText.name} = '$value'") {
            it.config.getOrNull(SemanticsProperties.EditableText)?.text.equals(value)
        }
    )

private fun ImageBitmap.assertCentered(excludedWidth: Int) {
    val pixel = toPixelMap()
    for (y in 0 until height) {
        for (x in 0 until (width - excludedWidth) / 2) {
            val leftPixel = pixel[x, y]
            pixel.assertPixelColor(leftPixel, width - 1 - x, y)
        }
    }
}