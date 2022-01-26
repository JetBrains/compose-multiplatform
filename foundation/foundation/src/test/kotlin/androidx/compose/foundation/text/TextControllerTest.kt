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

package androidx.compose.foundation.text

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalFoundationTextApi::class)
@RunWith(JUnit4::class)
class TextControllerTest {
    @Test
    fun `semantics modifier recreated when TextDelegate is set`() {
        val textDelegateBefore = mock<TextDelegate>()
        val textDelegateAfter = mock<TextDelegate>()
        // Make sure that mock doesn't do smart memory management:
        assertThat(textDelegateAfter).isNotSameInstanceAs(textDelegateBefore)

        val textState = TextState(textDelegateBefore, 0)
        val textController = TextController(textState)

        val semanticsModifierBefore = textController.semanticsModifier
        textController.setTextDelegate(textDelegateAfter)
        assertThat(textController.semanticsModifier).isNotSameInstanceAs(semanticsModifierBefore)
    }
}