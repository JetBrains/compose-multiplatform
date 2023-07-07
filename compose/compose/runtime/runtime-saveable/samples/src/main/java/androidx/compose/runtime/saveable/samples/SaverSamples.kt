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

@file:Suppress("UNUSED_VARIABLE")

package androidx.compose.runtime.saveable.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver

@Sampled
@Composable
fun CustomSaverSample() {
    data class Holder(var value: Int)

    // this Saver implementation converts Holder object which we don't know how to save
    // to Int which we can save
    val HolderSaver = Saver<Holder, Int>(
        save = { it.value },
        restore = { Holder(it) }
    )
}

@Sampled
@Composable
fun ListSaverSample() {
    data class Size(val x: Int, val y: Int)

    val sizeSaver = listSaver<Size, Int>(
        save = { listOf(it.x, it.y) },
        restore = { Size(it[0], it[1]) }
    )
}

@Sampled
@Composable
fun MapSaverSample() {
    data class User(val name: String, val age: Int)

    val userSaver = run {
        val nameKey = "Name"
        val ageKey = "Age"
        mapSaver(
            save = { mapOf(nameKey to it.name, ageKey to it.age) },
            restore = { User(it[nameKey] as String, it[ageKey] as Int) }
        )
    }
}
