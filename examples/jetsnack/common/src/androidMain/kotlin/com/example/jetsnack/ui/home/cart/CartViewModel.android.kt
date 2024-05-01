/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetsnack.ui.home.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetsnack.model.OrderLine
import com.example.jetsnack.model.SnackRepo
import com.example.jetsnack.model.SnackbarManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Factory for CartViewModel that takes SnackbarManager as a dependency
 */
fun CartViewModel.Companion.provideFactory(
    snackbarManager: SnackbarManager = SnackbarManager,
    snackRepository: SnackRepo = SnackRepo
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CartViewModel(snackbarManager, snackRepository) as T
    }
}

@OptIn(kotlin.ExperimentalMultiplatform::class)
actual abstract class JetSnackCartViewModel actual constructor() : ViewModel() {
    @Composable
    actual fun collectOrderLinesAsState(flow: StateFlow<List<OrderLine>>): State<List<OrderLine>> {
        return flow.collectAsStateWithLifecycle()
    }
}