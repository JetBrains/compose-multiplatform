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

package androidx.compose.ui.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.viewinterop.viewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

@Composable
fun ViewModelDemo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        val countViewModel = viewModel<CountViewModel>()
        val count by countViewModel.count.observeAsState()
        Text("Count is $count")
        Button(onClick = { countViewModel.increaseCount() }) {
            Text("Increase")
        }
    }
}

class CountViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val CountKey = "${javaClass.simpleName}-count"

    val count: LiveData<Int> =
        savedStateHandle.getLiveData(CountKey, 0)

    fun increaseCount() {
        savedStateHandle[CountKey] = (count.value ?: 0) + 1
    }
}
