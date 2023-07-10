/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement

@Suppress("UnstableApiUsage")
class ExperimentalPropertyAnnotationDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UAnnotation::class.java
    )

    override fun createUastHandler(context: JavaContext): UElementHandler = object :
        UElementHandler() {
        override fun visitAnnotation(node: UAnnotation) {
            val neededTargets = mutableSetOf(
                AnnotationUseSiteTarget.PROPERTY,
                AnnotationUseSiteTarget.PROPERTY_GETTER,
                AnnotationUseSiteTarget.PROPERTY_SETTER
            )

            // If this annotation is not annotated with an experimental annotation, return
            val resolved = node.resolve()
            if (BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS
                    .all { context.evaluator.getAnnotation(resolved, it) == null }) {
                return
            }

            val type = node.qualifiedName ?: return
            val source = node.sourcePsi as? KtAnnotationEntry ?: return

            // Check that the annotation is applied to a property
            val parent = source.parent?.parent
            if (parent !is KtProperty) return

            // Only applies to properties defined at the top level or in classes
            val propertyParent = parent.parent
            if ((propertyParent !is KtClassBody && propertyParent !is KtFile)) return

            // Don't apply the lint to private properties
            if (parent.isPrivate()) return
            // Don't apply the lint to properties in private classes
            if (propertyParent.getParentOfType<KtClass>(true)?.isPrivate() == true) return

            // Don't apply lint to const properties, because they are static fields in java
            if (parent.modifierList?.node?.findChildByType(KtTokens.CONST_KEYWORD) != null) return
            // Don't apply lint to @JvmField properties, because they are fields in java
            if (parent.annotationEntries.any { it.shortName.toString() == "JvmField" }) return

            // Don't apply lint to delegated properties
            if (parent.delegate != null) return

            // Annotation on setter is only needed for mutable property with non-private setter
            // Getter annotation is needed because the getter can't be private if the property isn't
            val setter = parent.setter
            if (!parent.isVar || (setter != null && setter.isPrivate())) {
                neededTargets.remove(AnnotationUseSiteTarget.PROPERTY_SETTER)
            }

            // Find all usages of this annotation on the property
            val existingTargets = parent.annotationEntries
                .filter { type.endsWith(it.shortName?.identifier ?: "") }
                .map { it.useSiteTarget?.getAnnotationUseSiteTarget() }

            val existingTargetSet = existingTargets
                // A null target means the default, which is the property target
                .map { it ?: AnnotationUseSiteTarget.PROPERTY }.toSet()
            val missingTargets = neededTargets - existingTargetSet

            if (missingTargets.isEmpty()) return

            // If not all annotations are present but more than one is, only report the error on
            // the first annotation to prevent duplicate errors
            val target = source.useSiteTarget?.getAnnotationUseSiteTarget()
            if (existingTargets.size > 1 && existingTargets.indexOf(target) != 0) return

            val fix = createFix(type, missingTargets)
            val message = "This property does not have all required annotations to correctly mark" +
                " it as experimental."
            val location = context.getLocation(node)
            val incident = Incident(ISSUE, node, location, message, fix)
            context.report(incident)
        }

        private fun createFix(
            annotation: String,
            missingTargets: Set<AnnotationUseSiteTarget>
        ): LintFix {
            val fix = fix()
                .name("Add missing annotations")
                .composite()

            for (target in missingTargets) {
                // There's a compilation error when an experimental annotation is applied to a
                // getter: https://kotlinlang.org/docs/opt-in-requirements.html#mark-api-elements
                // Add it anyway because metalava needs it and suppress the error
                if (target == AnnotationUseSiteTarget.PROPERTY_GETTER) {
                    val addSuppression = fix()
                        .annotate("kotlin.Suppress(\"OPT_IN_MARKER_ON_WRONG_TARGET\")")
                        .build()
                    fix.add(addSuppression)
                }

                val addAnnotation = fix()
                    // With replace = true, the existing annotation with a different target would
                    // be replaced. There shouldn't be an existing annotation with this target.
                    .annotate(target.renderName + ":" + annotation, replace = false)
                    .build()
                fix.add(addAnnotation)
            }

            return fix.build().autoFix()
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "ExperimentalPropertyAnnotation",
            "Experimental properties need to have annotations targeting the" +
                " property, getter, and (if applicable) setter.",
            "Annotations on Kotlin properties which don't specify a use-site will " +
                "only apply to the private backing field itself, and not to the getter or setter " +
                "(see https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets). " +
                "Annotating the property use-site is required by the Kotlin compiler, the get " +
                "use-site is required by Metalava, and the set use-site is required by Java " +
                "clients, so all use-sites must be annotated.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                ExperimentalPropertyAnnotationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
