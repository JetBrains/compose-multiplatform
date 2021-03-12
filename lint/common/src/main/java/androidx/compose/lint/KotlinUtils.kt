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

import com.intellij.psi.impl.compiled.ClsMethodImpl
import kotlinx.metadata.Flag
import org.jetbrains.kotlin.lexer.KtTokens.INLINE_KEYWORD
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.resolveToUElement

/**
 * @return whether the resolved declaration for this call expression is an inline function
 */
val UCallExpression.isDeclarationInline: Boolean
    get() {
        return when (val source = resolveToUElement()?.sourcePsi) {
            // Parsing a method defined in a class file
            is ClsMethodImpl -> {
                val flags = source.toKmFunction()?.flags ?: return false
                return Flag.Function.IS_INLINE(flags)
            }
            // Parsing a method defined in Kotlin source
            is KtFunction -> {
                source.hasModifier(INLINE_KEYWORD)
            }
            // Parsing another declaration (such as a property) which cannot be inline, or
            // a non-Kotlin declaration
            else -> false
        }
    }
