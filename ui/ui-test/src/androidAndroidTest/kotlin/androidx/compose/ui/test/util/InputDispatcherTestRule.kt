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

package androidx.compose.ui.test.util

import androidx.compose.ui.test.InputDispatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A test rule that modifies [InputDispatcher]s behavior. Can be used to disable dispatching
 * of MotionEvents in real time (skips the suspend before injection of an event) or to change
 * the time between consecutive injected events.
 *
 * @param eventPeriodOverride If set, specifies a different period in milliseconds between
 * two consecutive injected motion events injected by this [InputDispatcher]. If not
 * set, the event period of 10 milliseconds is unchanged.
 *
 * @see InputDispatcher.eventPeriodMillis
 */
internal class InputDispatcherTestRule(private val eventPeriodOverride: Long? = null) : TestRule {

    override fun apply(base: Statement, description: Description?): Statement {
        return ModifyingStatement(base)
    }

    inner class ModifyingStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            if (eventPeriodOverride != null) {
                InputDispatcher.eventPeriodMillis = eventPeriodOverride
            }
            try {
                base.evaluate()
            } finally {
                if (eventPeriodOverride != null) {
                    InputDispatcher.eventPeriodMillis = 10L
                }
            }
        }
    }
}