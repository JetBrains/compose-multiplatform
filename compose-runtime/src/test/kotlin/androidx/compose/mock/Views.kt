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

package androidx.compose.mock

import androidx.compose.Composable
import androidx.compose.emit
import androidx.compose.key

@Composable
fun <T : Any> MockComposeScope.repeat(
    of: Iterable<T>,
    block: @Composable MockComposeScope.(value: T) -> Unit
) {
    for (value in of) {
        key(value) {
            block(value)
        }
    }
}

@Composable
fun MockComposeScope.linear(block: @Composable MockComposeScope.() -> Unit) {
    emit<View, ViewApplier>(
        ctor = { View().also { it.name = "linear" } },
        update = { }
    ) {
        block()
    }
}

@Composable
fun MockComposeScope.text(value: String) {
    emit<View, ViewApplier>(
        ctor = { View().also { it.name = "text" } },
        update = { set(value) { text = it } }
    )
}

@Composable
fun MockComposeScope.edit(value: String) {
    emit<View, ViewApplier>(
        ctor = { View().also { it.name = "edit" } },
        update = { set(value) { this.value = it } }
    )
}

@Composable
fun MockComposeScope.selectBox(selected: Boolean, block: @Composable MockComposeScope.() -> Unit) {
    if (selected) {
        emit<View, ViewApplier>(
            ctor = { View().also { it.name = "box" } },
            update = { },
            children = { block() }
        )
    } else {
        block()
    }
}