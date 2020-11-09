/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

@Suppress("UNUSED_PARAMETER")
@Composable
private fun Text(text: String): Unit = TODO()

class ProduceStateSampleViewModel {
    val people: Flow<List<Person>> = TODO()

    interface Disposable {
        fun dispose()
    }

    @Suppress("UNUSED_PARAMETER")
    fun registerPersonObserver(observer: (Person) -> Unit): Disposable = TODO()
}

data class Person(val name: String)

private sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    class Data<T>(val data: T) : UiState<T>()
}

@Sampled
@Composable
fun ProduceState(viewModel: ProduceStateSampleViewModel) {
    val uiState by produceState<UiState<List<Person>>>(UiState.Loading, viewModel) {
        viewModel.people
            .map { UiState.Data(it) }
            .collect { value = it }
    }

    when (val state = uiState) {
        is UiState.Loading -> Text("Loading...")
        is UiState.Data -> Column {
            for (person in state.data) {
                Text("Hello, ${person.name}")
            }
        }
    }
}

@Suppress("UNUSED_VARIABLE")
@Sampled
@Composable
fun ProduceStateAwaitDispose(viewModel: ProduceStateSampleViewModel) {
    val currentPerson by produceState<Person?>(null, viewModel) {
        val disposable = viewModel.registerPersonObserver { person ->
            value = person
        }

        awaitDispose {
            disposable.dispose()
        }
    }
}