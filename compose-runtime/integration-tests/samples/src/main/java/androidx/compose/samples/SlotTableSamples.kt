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

@file:Suppress("unused", "UNUSED_PARAMETER")

package androidx.compose.samples

import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Sampled
import androidx.compose.Composable

@Sampled
@Composable
fun initialGroup() {
    LinearLayout {
        Contact(contact = jim)
        Contact(contact = bob)
    }
}

@Sampled
@Composable
fun reorderedGroup() {
    LinearLayout {
        Contact(contact = bob)
        Contact(contact = jim)
    }
}

@Sampled
@Composable
private fun contactSample() {
    @Composable
    fun Contact(contact: Contact) {
        TextView(text = contact.name)
        TextView(text = contact.email)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun Contact(contact: Contact) {}

private data class Contact(val name: String, val email: String)

private val jim = Contact("", "")
private val bob = Contact("", "")