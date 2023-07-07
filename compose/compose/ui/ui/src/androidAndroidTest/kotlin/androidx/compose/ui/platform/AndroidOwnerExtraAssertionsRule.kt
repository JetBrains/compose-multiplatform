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

package androidx.compose.ui.platform

import androidx.compose.ui.node.Owner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Enables extra assertions inside [Owner].
 */
class AndroidOwnerExtraAssertionsRule : TestRule {

    override fun apply(base: Statement, description: Description?): Statement {
        return ExtraValidationsStatement(base)
    }

    inner class ExtraValidationsStatement(
        private val base: Statement
    ) : Statement() {
        override fun evaluate() {
            Owner.enableExtraAssertions = true
            try {
                base.evaluate()
            } finally {
                Owner.enableExtraAssertions = false
            }
        }
    }
}
