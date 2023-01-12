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

package androidx.compose.runtime.rxjava3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins

/**
 * Subscribes to this [Observable] and represents its values via [State]. Every time there would
 * be new value posted into the [Observable] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Observable.onErrorReturn] or [Observable.onErrorResumeNext].
 *
 * @sample androidx.compose.runtime.rxjava3.samples.ObservableSample
 *
 * @param initial The initial value for the returned [State] which will be asynchronously updated
 * with the real one once we receive it from the stream
 */
@Suppress("UPPER_BOUND_VIOLATED")
@Composable
fun <R, T : R> Observable<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Flowable] and represents its values via [State]. Every time there would
 * be new value posted into the [Flowable] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Flowable.onErrorReturn] or [Flowable.onErrorResumeNext].
 *
 * @sample androidx.compose.runtime.rxjava3.samples.FlowableSample
 *
 * @param initial The initial value for the returned [State] which will be asynchronously updated
 * with the real one once we receive it from the stream
 */
@Suppress("UPPER_BOUND_VIOLATED")
@Composable
fun <R, T : R> Flowable<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Single] and represents its value via [State]. Once the value would be
 * posted into the [Single] the returned [State] will be updated causing recomposition of
 * every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Single.onErrorReturn] or [Single.onErrorResumeNext].
 *
 * @sample androidx.compose.runtime.rxjava3.samples.SingleSample
 *
 * @param initial The initial value for the returned [State] which will be asynchronously updated
 * with the real one once we receive it from the stream
 */
@Suppress("UPPER_BOUND_VIOLATED")
@Composable
fun <R, T : R> Single<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Maybe] and represents its value via [State]. Once the value would be
 * posted into the [Maybe] the returned [State] will be updated causing recomposition of
 * every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Maybe.onErrorComplete], [Maybe.onErrorReturn] or [Maybe.onErrorResumeNext].
 *
 * @sample androidx.compose.runtime.rxjava3.samples.MaybeSample
 *
 * @param initial The initial value for the returned [State] which will be asynchronously updated
 * with the real one once we receive it from the stream
 */
@Composable
fun <R, T : R> Maybe<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Completable] and represents its completed state via [State]. Once the
 * [Completable] will be completed the returned [State] will be updated with `true` value
 * causing recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Completable.onErrorComplete] or [Completable.onErrorResumeNext].
 *
 * @sample androidx.compose.runtime.rxjava3.samples.CompletableSample
 */
@Composable
fun Completable.subscribeAsState(): State<Boolean> =
    asState(false) { callback -> subscribe { callback(true) } }

@Composable
private inline fun <T, S> S.asState(
    initial: T,
    crossinline subscribe: S.((T) -> Unit) -> Disposable
): State<T> {
    val state = remember { mutableStateOf(initial) }
    DisposableEffect(this) {
        val disposable = subscribe {
            state.value = it
        }
        onDispose { disposable.dispose() }
    }
    return state
}