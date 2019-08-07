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

package androidx.compose.frames

class Address(street: String, city: String) : Framed {
    var street: String
        get() = next.readable(this).street
        set(value) { next.writable(this).street = value }

    var city: String
        get() = next.readable(this).city
        set(value) { next.writable(this).city = value }

    private var next: AddressRecord

    init {
        next = AddressRecord()
        next.street = street
        next.city = city
    }

    override fun prependFrameRecord(value: Record) {
        value.next = next
        next = value as AddressRecord
    }

    override val firstFrameRecord: AddressRecord get() = next
}

class AddressRecord : AbstractRecord() {
    var street: String = ""
    var city: String = ""

    override fun create(): Record = AddressRecord()
    override fun assign(value: Record) {
        val other = value as AddressRecord
        street = other.street
        city = other.city
    }
}
