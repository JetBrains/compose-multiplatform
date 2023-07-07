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

package androidx.compose.runtime.saveable

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MapSaverTest {

    @Test
    fun simpleSaveAndRestore() {
        val original = User("John", 30)
        val saved = with(UserSaver) {
            allowingScope.save(original)
        }

        assertThat(saved).isNotNull()
        assertThat(UserSaver.restore(saved!!))
            .isEqualTo(original)
    }

    @Test(expected = IllegalArgumentException::class)
    fun exceptionWhenAllItemsCantBeSaved() {
        with(UserSaver) {
            disallowingScope.save(User("John", 30))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun exceptionWhenOneValueCantBeSaved() {
        val onlyInts = object : SaverScope {
            override fun canBeSaved(value: Any) = value is Int
        }

        with(UserSaver) {
            onlyInts.save(User("John", 30))
        }
    }

    @Test
    fun nullableMapItemsAreSupported() {
        val original = NullableUser(null, 30)
        val saved = with(NullableUserSaver) {
            allowingScope.save(original)
        }

        assertThat(saved).isNotNull()
        assertThat(NullableUserSaver.restore(saved!!))
            .isEqualTo(original)
    }

    @Test
    fun nullableTypeIsSupported() {
        val saved = with(NullableUserSaver) {
            allowingScope.save(null)
        }

        assertThat(saved).isNotNull()
        assertThat(NullableUserSaver.restore(saved!!))
            .isEqualTo(NullableUser(null, null))
    }
}

private data class User(val name: String, val age: Int)

private val UserSaver = run {
    val nameKey = "Name"
    val ageKey = "Age"
    mapSaver(
        save = { mapOf(nameKey to it.name, ageKey to it.age) },
        restore = { User(it[nameKey] as String, it[ageKey] as Int) }
    )
}

private data class NullableUser(val name: String?, val age: Int?)

private val NullableUserSaver = run {
    val nameKey = "Name"
    val ageKey = "Age"
    mapSaver<NullableUser?>(
        save = { mapOf(nameKey to it?.name, ageKey to it?.age) },
        restore = { NullableUser(it[nameKey] as String?, it[ageKey] as Int?) }
    )
}