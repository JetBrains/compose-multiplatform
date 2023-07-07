/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.benchmark

import android.annotation.SuppressLint
import androidx.benchmark.junit4.BenchmarkRule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.testutils.recomposeAssertHadChanges
import androidx.compose.ui.Modifier

@SuppressLint("ModifierFactoryExtensionFunction")
fun repeatModifier(count: Int, mod: () -> Modifier): Modifier {
    var modifier: Modifier = Modifier
    repeat(count) {
        modifier = modifier.then(mod())
    }
    return modifier
}

/**
 * @param modifierFn A lambda which is passed a toggle parameter and is expected to return a
 *  modifier. The testcase will toggle the boolean passed in to this function, so the lambda
 *  should construct the lambda accordingly, depending on what is wanted to be tested. In practice,
 *  one shouldn't use this directly as it is a bit confusing, and instead use [measureModifier]
 *  which will construct this lambda accordingly.
 */
class ModifierTestCase(
    val modifierFn: (Boolean) -> Modifier,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private var state by mutableStateOf(true)

    override fun toggleState() {
        state = !state
    }

    @Composable
    override fun MeasuredContent() {
        Box(Modifier
            .fillMaxSize()
            .then(modifierFn(state))
        ) {
            Box(Modifier
                .fillMaxSize()
                .then(modifierFn(!state))
            )
        }
    }
}

/**
 * @param count - the number of modifiers on each node that we want to benchmark.
 * @param reuse - if true, the benchmark will toggle the parameter passed in to [modifierFn] with
 *  the expectation that it will create a similar modifier when it is toggled, but not one that is
 *  exactly equals with the previous one (ie, some params might be different). This will help give
 *  you an idea of what changing the modifier during recomposition might end up looking like.
 * @param hoistCreation - if true, the benchmark will not include the cost of running [modifierFn]
 *  itself.
 * @param includeComposition - if true, the composition cost will be included in the benchmark
 * @param includeLayout - if true, the cost of the layout phase will be included in the benchmark
 * @param includeDraw - if true, the cost of the draw phase will be included in the benchmark
 * @param modifierFn - A lambda which accepts a boolean and returns a modifier. The expectation is
 *  that this lambda wil call the modifier factory function directly, and pass in whatever
 *  necessary arguments. The boolean argument is meant to act as a toggle between two sets of
 *  consistent arguments. That is, if called twice with the same boolean value, the function should
 *  return modifiers with the same arguments both times, though it is expected that it will be a new
 *  instance. When called twice with different boolean values, it is expected that a similar
 *  modifier (ie, a modifier of the same type) is returned each time, however they would have two
 *  different sets of arguments, meaning that some update would need to take place.
 */
fun ComposeBenchmarkRule.measureModifier(
    count: Int = 1,
    reuse: Boolean = true,
    hoistCreation: Boolean = true,
    includeComposition: Boolean = true,
    includeLayout: Boolean = true,
    includeDraw: Boolean = true,
    modifierFn: (Boolean) -> Modifier,
) {
    val mfn: (Boolean) -> Modifier = when {
        !hoistCreation && reuse -> {
            { x: Boolean -> repeatModifier(count) { modifierFn(x) } }
        }
        hoistCreation && reuse -> {
            val left = repeatModifier(count) { modifierFn(true) }
            val right = repeatModifier(count) { modifierFn(false) };
            { x: Boolean -> if (x) left else right }
        }
        !hoistCreation && !reuse -> {
            { x: Boolean -> repeatModifier(count) { if (x) modifierFn(x) else Modifier } }
        }
        else /* hoistCreation && !reuse */ -> {
            val mod = repeatModifier(count) { modifierFn(true) };
            { x: Boolean -> if (x) mod else Modifier }
        }
    }
    runBenchmarkFor({ ModifierTestCase(mfn) }) {
        doFramesUntilNoChangesPending()

        measureRepeated {
            runWithTimingDisabled {
                getTestCase().toggleState()
            }
            timingIf(includeComposition) { recomposeAssertHadChanges() }
            assertNoPendingChanges()
            timingIf(includeLayout) {
                requestLayout()
                measure()
                layout()
            }
            timingIf(includeDraw) {
                drawPrepare()
                draw()
                drawFinish()
            }
        }
    }
}

inline fun BenchmarkRule.Scope.timingIf(condition: Boolean, block: () -> Unit) {
    if (condition) {
        block()
    } else {
        runWithTimingDisabled { block() }
    }
}