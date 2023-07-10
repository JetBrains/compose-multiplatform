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

package androidx

import android.os.Parcel
import android.os.Parcelable

@Suppress("unused", "UNUSED_PARAMETER")
open class ParcelableUsageKotlin protected constructor(parcel: Parcel) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }

    companion object CREATOR : Parcelable.Creator<ParcelableUsageKotlin> {
        override fun createFromParcel(parcel: Parcel): ParcelableUsageKotlin {
            return ParcelableUsageKotlin(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableUsageKotlin?> {
            return arrayOfNulls(size)
        }
    }
}