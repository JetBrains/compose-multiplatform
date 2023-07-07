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

package androidx.compose.runtime

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable

@SuppressLint("BanParcelableUsage")
internal class ParcelableSnapshotMutableState<T>(
    value: T,
    policy: SnapshotMutationPolicy<T>
) : SnapshotMutableStateImpl<T>(value, policy), Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(value)
        parcel.writeInt(
            when (policy) {
                neverEqualPolicy<Any?>() -> PolicyNeverEquals
                structuralEqualityPolicy<Any?>() -> PolicyStructuralEquality
                referentialEqualityPolicy<Any?>() -> PolicyReferentialEquality
                else -> throw IllegalStateException(
                    "Only known types of MutableState's SnapshotMutationPolicy are supported"
                )
            }
        )
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private const val PolicyNeverEquals = 0
        private const val PolicyStructuralEquality = 1
        private const val PolicyReferentialEquality = 2

        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableSnapshotMutableState<Any?>> =
            object : Parcelable.ClassLoaderCreator<ParcelableSnapshotMutableState<Any?>> {
                override fun createFromParcel(
                    parcel: Parcel,
                    loader: ClassLoader?
                ): ParcelableSnapshotMutableState<Any?> {
                    val value = parcel.readValue(loader ?: javaClass.classLoader)
                    val policyIndex = parcel.readInt()
                    return ParcelableSnapshotMutableState(
                        value,
                        when (policyIndex) {
                            PolicyNeverEquals -> neverEqualPolicy()
                            PolicyStructuralEquality -> structuralEqualityPolicy()
                            PolicyReferentialEquality -> referentialEqualityPolicy()
                            else -> throw IllegalStateException(
                                "Unsupported MutableState policy $policyIndex was restored"
                            )
                        }
                    )
                }

                override fun createFromParcel(parcel: Parcel) = createFromParcel(parcel, null)

                override fun newArray(size: Int) =
                    arrayOfNulls<ParcelableSnapshotMutableState<Any?>?>(size)
            }
    }
}
