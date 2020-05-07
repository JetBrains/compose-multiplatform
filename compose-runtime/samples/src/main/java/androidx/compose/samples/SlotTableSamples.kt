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

import androidx.annotation.Sampled
import androidx.compose.Composable

@Sampled
@Composable
fun initialGroup() {
    Column {
        Contact(contact = jim)
        Contact(contact = bob)
    }
}

@Sampled
@Composable
fun reorderedGroup() {
    Column {
        Contact(contact = bob)
        Contact(contact = jim)
    }
}

@Sampled
@Composable
private fun contactSample() {
    @Composable
    fun Contact(contact: Contact) {
        Text(text = contact.name)
        Text(text = contact.email)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun Contact(contact: Contact) {}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun Column(children: @Composable () -> Unit) {}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun Text(text: String) {}

private data class Contact(val name: String, val email: String)

private val jim = Contact("", "")
private val bob = Contact("", "")