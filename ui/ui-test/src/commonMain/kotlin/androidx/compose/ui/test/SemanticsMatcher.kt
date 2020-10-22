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

package androidx.compose.ui.test

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyKey

/**
 * Wrapper for semantics matcher lambdas that allows to build string explaining to the developer
 * what conditions were being tested.
 */
class SemanticsMatcher(
    val description: String,
    private val matcher: (SemanticsNode) -> Boolean
) {

    companion object {
        /**
         * Builds a predicate that tests whether the value of the given [key] is equal to
         * [expectedValue].
         */
        fun <T> expectValue(key: SemanticsPropertyKey<T>, expectedValue: T): SemanticsMatcher {
            return SemanticsMatcher("${key.name} = '$expectedValue'") {
                it.config.getOrElseNullable(key) { null } == expectedValue
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is defined in semantics.
         */
        fun <T> keyIsDefined(key: SemanticsPropertyKey<T>): SemanticsMatcher {
            return SemanticsMatcher("${key.name} is defined") {
                key in it.config
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is NOT defined in semantics.
         */
        fun <T> keyNotDefined(key: SemanticsPropertyKey<T>): SemanticsMatcher {
            return SemanticsMatcher("${key.name} is NOT defined") {
                key !in it.config
            }
        }
    }

    /**
     * Returns whether the given node is matched by this matcher.
     */
    fun matches(node: SemanticsNode): Boolean {
        return matcher(node)
    }

    /**
     * Returns whether at least one of the given nodes is matched by this matcher.
     */
    fun matchesAny(nodes: Iterable<SemanticsNode>): Boolean {
        return nodes.any(matcher)
    }

    infix fun and(other: SemanticsMatcher): SemanticsMatcher {
        return SemanticsMatcher("($description) && (${other.description})") {
            matcher(it) && other.matches(it)
        }
    }

    infix fun or(other: SemanticsMatcher): SemanticsMatcher {
        return SemanticsMatcher("($description) || (${other.description})") {
            matcher(it) || other.matches(it)
        }
    }

    operator fun not(): SemanticsMatcher {
        return SemanticsMatcher("NOT ($description)") {
            !matcher(it)
        }
    }
}