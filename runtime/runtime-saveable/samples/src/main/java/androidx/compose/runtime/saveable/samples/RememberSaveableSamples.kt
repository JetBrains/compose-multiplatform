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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Sampled
@Composable
fun RememberSaveable() {
    val list = rememberSaveable { mutableListOf<Int>() }
}

@Sampled
@Composable
fun RememberSaveableWithMutableState() {
    var value by rememberSaveable { mutableStateOf(({ "value" })()) }
}

@Sampled
@Composable
fun RememberSaveableCustomSaver() {
    val holder = rememberSaveable(saver = HolderSaver) { Holder(0) }
}

@Sampled
@Composable
fun RememberSaveableWithMutableStateAndCustomSaver() {
    val holder = rememberSaveable(stateSaver = HolderSaver) { mutableStateOf(Holder(0)) }
}

private data class Holder(var value: Int)

private val HolderSaver = Saver<Holder, Int>(
    save = { it.value },
    restore = { Holder(it) }
)