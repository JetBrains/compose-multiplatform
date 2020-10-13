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

package androidx.compose.foundation.text.demos

import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.integration.demos.common.DemoCategory

val TextDemos = DemoCategory(
    "Text",
    listOf(
        ComposableDemo("Static text") { TextDemo() },
        ComposableDemo("Text selection") { TextSelectionDemo() },
        ComposableDemo("Text selection sample") { TextSelectionSample() },
        ComposableDemo("Multi paragraph") { MultiParagraphDemo() },
        ComposableDemo("Interactive text") { InteractiveTextDemo() },
        DemoCategory(
            "Input fields",
            listOf(
                ComposableDemo("Basic input fields") { InputFieldDemo() },
                ComposableDemo("Various input fields") { VariousInputFieldDemo() },
                ComposableDemo("Tricky input field") { InputFieldTrickyUseCase() },
                ComposableDemo("Focus transition") { TextFieldFocusTransition() },
                ComposableDemo("Tail Following Text Field") { TailFollowingTextFieldDemo() },
                ComposableDemo("TextField in Scroller") { TextFieldWithScrollerDemo() },
                ComposableDemo("Soft Wrap") { SoftWrapDemo() },
            )
        ),
        ComposableDemo("Text Accessibility") { TextAccessibilityDemo() }
    )
)