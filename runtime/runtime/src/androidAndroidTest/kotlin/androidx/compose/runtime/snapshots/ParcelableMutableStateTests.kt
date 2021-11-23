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

package androidx.compose.runtime.snapshots

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.structuralEqualityPolicy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ParcelableMutableStateTests(
    private val policy: SnapshotMutationPolicy<Int>
) {
    @Test
    fun saveAndRestoreTheMutableStateOf() {
        val a = mutableStateOf(0, policy)
        a.value = 1

        val parcel = Parcel.obtain()
        parcel.writeParcelable(a as Parcelable, 0)
        parcel.setDataPosition(0)
        @Suppress("UNCHECKED_CAST", "deprecation")
        val restored =
            parcel.readParcelable<Parcelable>(javaClass.classLoader) as SnapshotMutableState<Int>

        assertEquals(1, restored.value)
        assertEquals(policy, restored.policy)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<SnapshotMutationPolicy<Int>> =
            arrayOf(
                structuralEqualityPolicy(),
                referentialEqualityPolicy(),
                neverEqualPolicy()
            )
    }
}
