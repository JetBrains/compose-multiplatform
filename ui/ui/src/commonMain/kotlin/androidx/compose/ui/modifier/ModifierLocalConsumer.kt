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

package androidx.compose.ui.modifier

import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import kotlin.jvm.JvmDefaultWithCompatibility

/**
 * A Modifier that can be used to consume [ModifierLocal]s that were provided by other modifiers to
 * the left of this modifier, or above this modifier in the layout tree.
 */
@Stable
@JvmDefaultWithCompatibility
interface ModifierLocalConsumer : Modifier.Element {
    /**
     * This function is called whenever one of the consumed values has changed.
     * This could be called in response to the modifier being added, removed or re-ordered.
     */
    fun onModifierLocalsUpdated(scope: ModifierLocalReadScope)
}

/**
 * A Modifier that can be used to consume [ModifierLocal]s that were provided by other modifiers to
 * the left of this modifier, or above this modifier in the layout tree.
 */
@Stable
@ExperimentalComposeUiApi
fun Modifier.modifierLocalConsumer(consumer: ModifierLocalReadScope.() -> Unit): Modifier {
    return this.then(
        ModifierLocalConsumerImpl(
            consumer,
            debugInspectorInfo {
                name = "modifierLocalConsumer"
                properties["consumer"] = consumer
            }
        )
    )
}

@Stable
private class ModifierLocalConsumerImpl(
    val consumer: ModifierLocalReadScope.() -> Unit,
    debugInspectorInfo: InspectorInfo.() -> Unit
) : ModifierLocalConsumer, InspectorValueInfo(debugInspectorInfo) {

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) {
        consumer.invoke(scope)
    }

    override fun equals(other: Any?): Boolean {
        return other is ModifierLocalConsumerImpl && other.consumer == consumer
    }

    override fun hashCode() = consumer.hashCode()
}