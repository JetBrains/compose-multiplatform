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

package androidx.compose.ui.test.junit4

import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.transition
import androidx.compose.animation.transitionsEnabled
import androidx.compose.ui.test.InternalTestApi
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule will disable all [transition] animations for the test. As a convenience, the rule
 * can be turned into a no-op by setting [disableTransitions] to `false`, allowing you to put it
 * into a rule chain without branching logic.
 */
@InternalTestApi
class DisableTransitionsTestRule(private val disableTransitions: Boolean = false) : TestRule {

    override fun apply(base: Statement, description: Description?): Statement {
        return if (disableTransitions) DisableTransitionsStatement(base) else base
    }

    private class DisableTransitionsStatement(private val base: Statement) : Statement() {
        @Suppress("DEPRECATION_ERROR")
        @OptIn(InternalAnimationApi::class)
        override fun evaluate() {
            transitionsEnabled = false
            try {
                base.evaluate()
            } finally {
                transitionsEnabled = true
            }
        }
    }
}

@Deprecated(
    message = "Renamed to DisableTransitionsTestRule",
    replaceWith = ReplaceWith(
        "DisableTransitionsTestRule",
        "androidx.ui.test.DisableTransitionsTestRule"
    )
)
@Suppress("unused")
@OptIn(InternalTestApi::class)
typealias DisableTransitions = DisableTransitionsTestRule
