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

package androidx.compose.ui.platform

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.Context
import androidx.compose.ui.text.AnnotatedString

private const val PLAIN_TEXT_LABEL = "plain text"

/**
 * Android implementation for [ClipboardManager].
 */
internal class AndroidClipboardManager(context: Context) : ClipboardManager {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as
        android.content.ClipboardManager

    override fun setText(annotatedString: AnnotatedString) {
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                PLAIN_TEXT_LABEL,
                convertAnnotatedStringToCharSequence(annotatedString)
            )
        )
    }

    override fun getText(): AnnotatedString? {
        return if (clipboardManager.hasPrimaryClip())
            convertCharSequenceToAnnotatedString(clipboardManager.primaryClip!!.getItemAt(0).text)
        else null
    }

    fun hasText() =
        clipboardManager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) ?: false

    private fun convertCharSequenceToAnnotatedString(charSequence: CharSequence?):
        AnnotatedString? {
            if (charSequence == null) return null
            return AnnotatedString(text = charSequence.toString())
        }

    private fun convertAnnotatedStringToCharSequence(annotatedString: AnnotatedString):
        CharSequence? {
            return annotatedString.text
        }
}
