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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UClass

class PrivateConstructorForUtilityClassDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {
            // If this doesn't look like a utility class then return.
            if (node.isInterface ||
                node.isEnum ||
                node.hasModifierProperty(PsiModifier.ABSTRACT) ||
                node is UAnonymousClass ||
                // If this is a subclass, then don't flag it.
                node.supers.any { !it.qualifiedName.equals("java.lang.Object") } ||
                // Don't run for Kotlin, for now at least
                node.containingFile.fileType == KotlinFileType.INSTANCE
            ) {
                return
            }

            // If the constructors are already private or the class is private with a default
            // constructor (e.g. no constructors) then return.
            if (node.constructors.isNotEmpty() && node.constructors.all { it.isPrivate() } ||
                node.constructors.isEmpty() && node.isPrivate()
            ) {
                return
            }

            // If not all (non-constructor) members are static then return.
            if (node.methods.any { !it.isStatic && !it.isConstructor } ||
                node.methods.none { !it.isConstructor } ||
                node.fields.any { !it.isStatic }
            ) {
                return
            }

            val incident = Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(node))
                .message("Utility class is missing private constructor")
                .scope(node)
            context.report(incident)
        }
    }

    private fun PsiModifierListOwner.isPrivateOrParameterInPrivateMethod(): Boolean {
        if (hasModifier(JvmModifier.PRIVATE)) return true
        val parentMethod = (this as? PsiParameter)?.declarationScope as? PsiMethod ?: return false
        return parentMethod.hasModifier(JvmModifier.PRIVATE)
    }

    /**
     * Returns whether the element is private.
     */
    fun PsiModifierListOwner.isPrivate(): Boolean = hasModifier(JvmModifier.PRIVATE)

    companion object {
        val ISSUE = Issue.create(
            "PrivateConstructorForUtilityClass",
            "Utility classes should have a private constructor",
            "Classes which are not intended to be instantiated should be made non-instantiable " +
                "with a private constructor. This includes utility classes (classes with " +
                "only static members), and the main class.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                PrivateConstructorForUtilityClassDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
