/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.convertWithParent
import org.jetbrains.uast.toUElement

/**
 * Detects subclasses of ModifierNodeElement that do not override the base `inspectableProperties`
 * function. Classes that extend from another class which overrides this function satisfy the
 * inspection.
 *
 * We suggest overriding this method to provide more accurate, complete, and consistent data in
 * the layout inspector. This check reports an error for AndroidX libraries, since we want to ensure
 * each modifier has full inspection support. Additionally, the base implementation can only work
 * if kotlin-reflect is in the classpath, which is avoided by having a custom implementation.
 */
class ModifierNodeInspectablePropertiesDetector : Detector(), Detector.UastScanner {

    override fun applicableSuperClasses(): List<String> {
        return listOf("androidx.compose.ui.node.ModifierNodeElement")
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (declaration.qualifiedName == ModifierNodeElementFqName) {
            return
        }

        if (declaration.getInspectablePropertiesFunctionOverride() == null) {
            context.report(
                ModifierNodeInspectableProperties,
                declaration,
                context.getNameLocation(declaration),
                "${declaration.name} does not override $InspectionFunName(). The layout " +
                    "inspector will use the default implementation of this function, which " +
                    "will attempt to read ${declaration.name}'s properties reflectively. " +
                    "Override $InspectionFunName() if you'd like to customize this modifier's " +
                    "presentation in the layout inspector."
            )
        }
    }

    private tailrec fun UClass?.getInspectablePropertiesFunctionOverride(): UMethod? {
        if (this == null || qualifiedName == ModifierNodeElementFqName) {
            return null
        }

        return uastDeclarations
            .filterIsInstance<UMethod>()
            .firstOrNull { it.hasInspectablePropertiesSignature() }
            ?: UastFacade.convertWithParent<UClass>(javaPsi.superClass)
                ?.getInspectablePropertiesFunctionOverride()
    }

    private fun UMethod.hasInspectablePropertiesSignature(): Boolean {
        return name == InspectionFunName &&
            parameters.size == 1 && // The only argument is the receiver
            receiverFqType == InspectionFunFqReceiver
    }

    private val UMethod.receiverFqType: String?
        get() {
            val receiverTypeRef = (sourcePsi as? KtCallableDeclaration)?.receiverTypeReference
            return (receiverTypeRef?.toUElement() as? UTypeReferenceExpression)?.getQualifiedName()
        }

    companion object {
        private const val ModifierNodeElementFqName = "androidx.compose.ui.node.ModifierNodeElement"
        private const val InspectionFunName = "inspectableProperties"
        private const val InspectionFunFqReceiver = "androidx.compose.ui.platform.InspectorInfo"

        val ModifierNodeInspectableProperties = Issue.create(
            "ModifierNodeInspectableProperties",
            "ModifierNodeElement missing inspectableProperties",
            "ModifierNodeElements may override inspectableProperties() to provide information " +
                "about the modifier in the layout inspector. The default implementation attempts " +
                "to read all of the properties on the class reflectively, which may not " +
                "comprehensively or effectively describe the modifier.",
            Category.PRODUCTIVITY, 4, Severity.INFORMATIONAL,
            Implementation(
                ModifierNodeInspectablePropertiesDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}