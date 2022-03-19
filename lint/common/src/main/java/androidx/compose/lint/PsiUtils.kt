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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil

/**
 * Returns whether [this] has [packageName] as its package name.
 */
fun PsiMethod.isInPackageName(packageName: PackageName): Boolean {
    val actual = (containingFile as? PsiJavaFile)?.packageName
    return packageName.javaPackageName == actual
}

/**
 * Whether this [PsiMethod] returns Unit
 */
val PsiMethod.returnsUnit
    get() = returnType.isVoidOrUnit

/**
 * Whether this [PsiType] is `void` or [Unit]
 *
 * In Kotlin 1.6 some expressions now explicitly return [Unit] instead of just being [PsiType.VOID],
 * so this returns whether this type is either.
 */
val PsiType?.isVoidOrUnit
    get() = this == PsiType.VOID || this?.canonicalText == "kotlin.Unit"

/**
 * @return whether [this] inherits from [name]. Returns `true` if [this] _is_ directly [name].
 */
fun PsiType.inheritsFrom(name: Name) =
    InheritanceUtil.isInheritor(this, name.javaFqn)

/**
 * @return whether [this] inherits from [name]. Returns `true` if [this] _is_ directly [name].
 */
fun PsiClass.inheritsFrom(name: Name) =
    InheritanceUtil.isInheritor(this, name.javaFqn)
