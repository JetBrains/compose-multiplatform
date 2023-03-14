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

package androidx.compose.ui.input

import android.view.Choreographer
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.InputMethodManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.text.input.asExecutor
import androidx.emoji2.text.EmojiCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextInputServiceAndroidEmojiTest {
    @After
    fun cleanup() {
        EmojiCompat.reset(null)
    }

    @Test
    fun whenEmojiCompat_addsEditorInfo() {
        val e2 = mock<EmojiCompat>()
        EmojiCompat.reset(e2)
        val view = View(InstrumentationRegistry.getInstrumentation().context)
        val inputMethodManager = mock<InputMethodManager>()
        // Choreographer must be retrieved on main thread.
        val choreographer = Espresso.onIdle { Choreographer.getInstance() }
        val textInputService = TextInputServiceAndroid(
            view,
            inputMethodManager,
            inputCommandProcessorExecutor = choreographer.asExecutor()
        )

        textInputService.startInput(TextFieldValue(""), ImeOptions.Default, {}, {})

        val info = EditorInfo()
        textInputService.createInputConnection(info)
        verify(e2).updateEditorInfo(eq(info))
    }
}