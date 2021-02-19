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

import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UndoManagerTest {
    @Test
    fun basic_undo_redo() {
        val manager = UndoManager(10)
        manager.makeSnapshot(TextFieldValue("hi"))
        manager.makeSnapshot(TextFieldValue("hello"))
        assertThat(manager.undo()).isEqualTo(
            TextFieldValue("hi")
        )
        assertThat(manager.undo()).isNull()
        assertThat(manager.redo()).isEqualTo(
            TextFieldValue("hello")
        )
        assertThat(manager.redo()).isNull()
        assertThat(manager.undo()).isEqualTo(
            TextFieldValue("hi")
        )
        manager.makeSnapshot(TextFieldValue("hola"))
        assertThat(manager.undo()).isEqualTo(
            TextFieldValue("hi")
        )
        assertThat(manager.redo()).isEqualTo(
            TextFieldValue("hola")
        )
    }

    @Test
    fun max_size_too_small() {
        val manager = UndoManager(10)
        manager.makeSnapshot(TextFieldValue("hi"))
        manager.makeSnapshot(TextFieldValue("hello"))
        manager.makeSnapshot(TextFieldValue("hola"))
        assertThat(manager.undo()).isEqualTo(
            TextFieldValue("hello")
        )
        assertThat(manager.undo()).isNull()
        assertThat(manager.redo()).isEqualTo(
            TextFieldValue("hola")
        )
    }

    @Test
    fun max_size_enough() {
        val manager = UndoManager(11)
        manager.makeSnapshot(TextFieldValue("hi"))
        manager.makeSnapshot(TextFieldValue("hello"))
        manager.makeSnapshot(TextFieldValue("hola"))
        assertThat(manager.undo()).isEqualTo(
            TextFieldValue("hello")
        )
        assertThat(manager.undo()).isEqualTo(
            TextFieldValue("hi")
        )
    }
}