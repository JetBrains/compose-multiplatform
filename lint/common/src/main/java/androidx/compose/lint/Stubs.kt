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

package androidx.compose.lint

import org.intellij.lang.annotations.Language

/**
 * Common Compose-related lint stubs used for testing
 */
object Stubs {
    val Composable = stub(
        """
        package androidx.compose.runtime

        @MustBeDocumented
        @Retention(AnnotationRetention.BINARY)
        @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
            AnnotationTarget.PROPERTY
        )
        annotation class Composable
        """
    )

    val Modifier = stub(
        """
        package androidx.compose.ui

        import androidx.compose.ui.platform.InspectorInfo
        import androidx.compose.ui.platform.InspectorValueInfo

        interface Modifier {
          infix fun then(other: Modifier): Modifier =
              if (other === Modifier) this else CombinedModifier(this, other)

          interface Element : Modifier

          companion object : Modifier {
            override infix fun then(other: Modifier): Modifier = other
          }
        }

        class CombinedModifier(
            private val outer: Modifier,
            private val inner: Modifier
        ) : Modifier
        """
    )

    val Remember = stub(
        """
        package androidx.compose.runtime

        import androidx.compose.runtime.Composable

        @Composable
        inline fun <T> remember(calculation: () -> T): T = calculation()

        @Composable
        inline fun <T, V1> remember(
            v1: V1,
            calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <T, V1, V2> remember(
            v1: V1,
            v2: V2,
            calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <T, V1, V2, V3> remember(
            v1: V1,
            v2: V2,
            v3: V3,
            calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <V> remember(
            vararg inputs: Any?,
            calculation: () -> V
        ): V = calculation()
        """
    )
}

// @Language isn't available as a type annotation, so we need a parameter
private fun stub(@Language("kotlin") code: String) = code