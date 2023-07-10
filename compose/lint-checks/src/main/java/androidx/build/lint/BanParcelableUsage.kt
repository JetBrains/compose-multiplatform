/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UClass
import java.util.Collections

const val PARCELABLE_INTERFACE_CANONICAL_NAME = "android.os.Parcelable"

class BanParcelableUsage : Detector(), Detector.UastScanner {

    override fun applicableSuperClasses(): List<String>? {
        return Collections.singletonList(PARCELABLE_INTERFACE_CANONICAL_NAME)
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (declaration.isInterface ||
            declaration.hasModifierProperty(PsiModifier.ABSTRACT) ||
            declaration is UAnonymousClass
        ) {
            return
        }
        // For now only find classes that directly implement Parcelable, because
        // lint will also examine the entire inheritance and implementation chain.
        for (superclass in declaration.uastSuperTypes) {
            if (superclass.type.canonicalText == PARCELABLE_INTERFACE_CANONICAL_NAME) {
                val incident = Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(declaration))
                    .message("Class implements android.os.Parcelable")
                    .scope(declaration)
                context.report(incident)
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "BanParcelableUsage",
            "Class implements android.os.Parcelable",
            "Use of Parcelable is no longer recommended," +
                " please use VersionedParcelable instead.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(BanParcelableUsage::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
