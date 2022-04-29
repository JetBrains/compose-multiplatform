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

package androidx.compose.foundation.demos.text

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

val TextDemos = DemoCategory(
    "Text",
    listOf(
        ComposableDemo("Static text") { TextDemo() },
        ComposableDemo("Brush") { TextBrushDemo() },
        ComposableDemo("Ellipsize") { EllipsizeDemo() },
        ComposableDemo("Typeface") { TypefaceDemo() },
        ComposableDemo("FontFamily fallback") { FontFamilyDemo() },
        ComposableDemo("All system font families") { SystemFontFamilyDemo() },
        ComposableDemo("Text selection") { TextSelectionDemo() },
        ComposableDemo("Text selection sample") { TextSelectionSample() },
        ComposableDemo("Multi paragraph") { MultiParagraphDemo() },
        ComposableDemo("IncludeFontPadding & Clip") { TextFontPaddingDemo() },
        ComposableDemo("Layout Reuse") { TextReuseLayoutDemo() },
        ComposableDemo("Line Height Behavior") { TextLineHeightDemo() },
        ComposableDemo("Interactive text") { InteractiveTextDemo() },
        DemoCategory(
            "Input fields",
            listOf(
                ComposableDemo("Basic input fields") { InputFieldDemo() },
                ComposableDemo("Keyboard Types") { KeyboardTypeDemo() },
                ComposableDemo("Ime Action") { ImeActionDemo() },
                ComposableDemo("Various input fields") { VariousInputFieldDemo() },
                ComposableDemo("Tricky input field") { InputFieldTrickyUseCase() },
                ComposableDemo("Focus transition") { TextFieldFocusTransition() },
                ComposableDemo("Focus keyboard interaction") {
                    TextFieldFocusKeyboardInteraction()
                },
                ComposableDemo("Tail Following Text Field") { TailFollowingTextFieldDemo() },
                ComposableDemo("Scrollable text fields") { ScrollableTextFieldDemo() },
                ComposableDemo("Min/Max Lines") { BasicTextFieldMinMaxDemo() },
                ComposableDemo("Ime SingleLine") { ImeSingleLineDemo() },
                ComposableDemo("Capitalization/AutoCorrect") { CapitalizationAutoCorrectDemo() },
                ComposableDemo("TextFieldValue") { TextFieldValueDemo() },
                ComposableDemo("Inside Dialog") { onNavigateUp ->
                    DialogInputFieldDemo(onNavigateUp)
                },
                ComposableDemo("Inside scrollable") { TextFieldsInScrollableDemo() }
            )
        ),
        ComposableDemo("Text Accessibility") { TextAccessibilityDemo() }
    )
)