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

package androidx.compose.runtime.rxjava3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava3.subscribeAsState
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Sampled
@Composable
fun ObservableSample(observable: Observable<String>) {
    val value: String by observable.subscribeAsState("initial")
    Text("Value is $value")
}

@Sampled
@Composable
fun FlowableSample(flowable: Flowable<String>) {
    val value: String by flowable.subscribeAsState("initial")
    Text("Value is $value")
}

@Sampled
@Composable
fun SingleSample(single: Single<String>) {
    val value: String by single.subscribeAsState("initial")
    Text("Value is $value")
}

@Sampled
@Composable
fun MaybeSample(maybe: Maybe<String>) {
    val value: String by maybe.subscribeAsState("initial")
    Text("Value is $value")
}

@Sampled
@Composable
fun CompletableSample(completable: Completable) {
    val completed by completable.subscribeAsState()
    Text("Completable is $completed")
}
