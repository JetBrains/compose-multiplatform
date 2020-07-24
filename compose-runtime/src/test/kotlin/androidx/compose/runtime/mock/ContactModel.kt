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

package androidx.compose.runtime.mock

class ContactModel(
    var filter: String = "",
    val contacts: MutableList<Contact>,
    var selected: Contact? = null
) {
    val filtered get() = contacts.filter { it.name.contains(filter) }

    fun add(contact: Contact, after: Contact? = null) {
        if (after == null) {
            contacts.add(contact)
        } else {
            contacts.add(find(after) + 1, contact)
        }
    }

    fun move(contact: Contact, after: Contact?) {
        if (after == null) {
            contacts.removeAt(find(contact))
            contacts.add(0, contact)
        } else {
            contacts.removeAt(find(contact))
            contacts.add(find(after) + 1, contact)
        }
    }

    private fun find(contact: Contact): Int {
        val index = contacts.indexOf(contact)
        if (index < 0) error("Contact $contact not found")
        return index
    }
}
