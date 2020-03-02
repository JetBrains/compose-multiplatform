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

package androidx.compose

internal expect fun UiComposer(
    context: Context,
    root: Any,
    slots: SlotTable,
    recomposer: Recomposer
): Composer<*>

// TODO(b/147710889): Once composer param work is complete, this API should be removed and
//  replaced with an internal API used for invoking composables.
/**
 * Execute a block of code with the composer in "composing" mode. After executing, the composer
 * will revert back to it's previous "composing" state. This can be useful for manually starting
 * a composition.
 *
 * @param block the code to execute
 */
@TestOnly
expect fun <T> Composer<*>.runWithComposing(block: () -> T): T

@PublishedApi
internal val invocation = OpaqueKey("invocation")

@PublishedApi
internal val provider = OpaqueKey("provider")

@PublishedApi
internal val providerValues = OpaqueKey("providerValues")

@PublishedApi
internal val providerMaps = OpaqueKey("providerMaps")

@PublishedApi
internal val consumer = OpaqueKey("consumer")

@PublishedApi
internal val reference = OpaqueKey("reference")