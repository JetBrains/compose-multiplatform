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

package androidx.compose.ui.lint

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isComposable
import androidx.compose.lint.toKmFunction
import androidx.compose.ui.lint.ModifierDeclarationDetector.Companion.ComposableModifierFactory
import androidx.compose.ui.lint.ModifierDeclarationDetector.Companion.ModifierFactoryReturnType
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.impl.compiled.ClsMethodImpl
import java.util.EnumSet
import kotlinx.metadata.KmClassifier
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks functions returning Modifiers for consistency with guidelines.
 *
 * - Modifier factory functions should return Modifier as their type, and not a subclass of Modifier
 * - Modifier factory functions should be defined as an extension on Modifier to allow fluent
 * chaining
 * - Modifier factory functions should not be marked as @Composable, and should use `composed`
 * instead
 * - Modifier factory functions should reference the receiver parameter inside their body to make
 * sure they don't drop old Modifiers in the chain
 */
class ModifierDeclarationDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            // Ignore functions that do not return
            val returnType = node.returnType ?: return

            // Ignore functions that do not return Modifier or something implementing Modifier
            if (!returnType.inheritsFrom(Names.Ui.Modifier)) return

            // Ignore ParentDataModifiers - this is a special type of Modifier where the type is
            // used to provide data for use in layout, so we don't want to warn here.
            if (returnType.inheritsFrom(Names.Ui.Layout.ParentDataModifier)) return

            val source = node.sourcePsi

            // If this node is a property that is a constructor parameter, ignore it.
            if (source is KtParameter) return

            // Ignore properties in some cases
            if (source is KtProperty) {
                // If this node is inside a class or object, ignore it.
                if (source.containingClassOrObject != null) return
                // If this node is a var, ignore it.
                if (source.isVar) return
                // If this node is a val with no getter, ignore it.
                if (source.getter == null) return
            }
            if (source is KtPropertyAccessor) {
                // If this node is inside a class or object, ignore it.
                if (source.property.containingClassOrObject != null) return
                // If this node is a getter on a var, ignore it.
                if (source.property.isVar) return
            }

            node.checkComposability(context)
            node.checkReturnType(context, returnType)
            node.checkReceiver(context)
        }
    }

    companion object {
        val ComposableModifierFactory = Issue.create(
            "ComposableModifierFactory",
            "Modifier factory functions should not be @Composable",
            "Modifier factory functions that need to be aware of the composition should use " +
                "androidx.compose.ui.composed {} in their implementation instead of being marked " +
                "as @Composable. This allows Modifiers to be referenced in top level variables " +
                "and constructed outside of the composition.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ModifierDeclarationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val ModifierFactoryReturnType = Issue.create(
            "ModifierFactoryReturnType",
            "Modifier factory functions should return Modifier",
            "Modifier factory functions should return Modifier as their type, and not a " +
                "subtype of Modifier (such as Modifier.Element).",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ModifierDeclarationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val ModifierFactoryExtensionFunction = Issue.create(
            "ModifierFactoryExtensionFunction",
            "Modifier factory functions should be extensions on Modifier",
            "Modifier factory functions should be defined as extension functions on" +
                " Modifier to allow modifiers to be fluently chained.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                ModifierDeclarationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )

        val ModifierFactoryUnreferencedReceiver = Issue.create(
            "ModifierFactoryUnreferencedReceiver",
            "Modifier factory functions must use the receiver Modifier instance",
            "Modifier factory functions are fluently chained to construct a chain of " +
                "Modifier objects that will be applied to a layout. As a result, each factory " +
                "function *must* use the receiver `Modifier` parameter, to ensure that the " +
                "function is returning a chain that includes previous items in the chain. Make " +
                "sure the returned chain either explicitly includes `this`, such as " +
                "`return this.then(MyModifier)` or implicitly by returning a chain that starts " +
                "with an implicit call to another factory function, such as " +
                "`return myModifier()`, where `myModifier` is defined as " +
                "`fun Modifier.myModifier(): Modifier`.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ModifierDeclarationDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

/**
 * @see [ModifierDeclarationDetector.ComposableModifierFactory]
 */
private fun UMethod.checkComposability(context: JavaContext) {
    if (isComposable) {
        val source = sourcePsi as KtDeclarationWithBody

        val replaceWhitespaceRegex = "[\\s\\t\\n\\r]+"

        val body = source.bodyExpression!!.text

        val newBody = if (source.hasBlockBody()) {
            "= composed " + body.replace("return$replaceWhitespaceRegex".toRegex(), "")
        } else {
            "composed { $body }"
        }

        val scope = if (source is KtPropertyAccessor) source.property else source

        val functionWithoutComposable = scope.text
            .replaceFirst("@Composable$replaceWhitespaceRegex".toRegex(), "")
            .replaceFirst("@get:Composable$replaceWhitespaceRegex".toRegex(), "")

        val newFunction = functionWithoutComposable.replace(body, newBody)
        context.report(
            ComposableModifierFactory,
            this,
            context.getNameLocation(this),
            "Modifier factory functions should not be marked as @Composable, and should " +
                "use composed instead",
            LintFix.create()
                .replace()
                .name("Replace @Composable with composed call")
                .range(context.getLocation(scope))
                .all()
                .with(newFunction)
                .autoFix()
                .build()
        )
    }
}

/**
 * @see [ModifierDeclarationDetector.ModifierFactoryExtensionFunction]
 */
private fun UMethod.checkReceiver(context: JavaContext) {
    fun report(lintFix: LintFix? = null) {
        context.report(
            ModifierDeclarationDetector.ModifierFactoryExtensionFunction,
            this,
            context.getNameLocation(this),
            "Modifier factory functions should be extensions on Modifier",
            lintFix
        )
    }

    val source = when (val source = sourcePsi) {
        is KtFunction -> source
        is KtPropertyAccessor -> source.property
        else -> return
    }

    val receiverTypeReference = source.receiverTypeReference

    // No receiver
    if (receiverTypeReference == null) {
        val name = source.nameIdentifier!!.text
        report(
            LintFix.create()
                .replace()
                .name("Add Modifier receiver")
                .range(context.getLocation(source))
                .text(name)
                .with("${Names.Ui.Modifier.shortName}.$name")
                .autoFix()
                .build()
        )
    } else {
        val receiverType = (receiverTypeReference.typeElement as KtUserType)
        val receiverShortName = receiverType.referencedName
        // Try to resolve the class definition of the receiver
        val receiverFqn = (
            receiverType.referenceExpression.toUElement()?.tryResolve().toUElement() as? PsiClass
            )?.qualifiedName
        val hasModifierReceiver = if (receiverFqn != null) {
            // If we could resolve the class, match fqn
            receiverFqn == Names.Ui.Modifier.javaFqn
        } else {
            // Otherwise just try and match the short names
            receiverShortName == Names.Ui.Modifier.shortName
        }
        if (!hasModifierReceiver) {
            report(
                LintFix.create()
                    .replace()
                    .name("Change receiver to Modifier")
                    .range(context.getLocation(source))
                    .text(receiverShortName)
                    .with(Names.Ui.Modifier.shortName)
                    .autoFix()
                    .build()
            )
        } else {
            // Ignore interface / abstract methods with no body
            if (uastBody != null) {
                ensureReceiverIsReferenced(context)
            }
        }
    }
}

/**
 * See [ModifierDeclarationDetector.ModifierFactoryUnreferencedReceiver]
 */
private fun UMethod.ensureReceiverIsReferenced(context: JavaContext) {
    var isReceiverReferenced = false
    accept(object : AbstractUastVisitor() {
        /**
         * If there is no receiver on the call, but the call has a Modifier receiver
         * type, then the call is implicitly using the Modifier receiver
         * TODO: consider checking for nested receivers, in case the implicit
         *  receiver is an inner scope, and not the outer Modifier receiver
         */
        override fun visitCallExpression(node: UCallExpression): Boolean {
            // We account for a receiver of `this` in `visitThisExpression`
            if (node.receiver == null) {
                val declaration = node.resolveToUElement()
                // If the declaration is a member of `Modifier` (such as `then`)
                if (declaration?.getContainingUClass()
                    ?.qualifiedName == Names.Ui.Modifier.javaFqn
                ) {
                    isReceiverReferenced = true
                    // Otherwise if the declaration is an extension of `Modifier`
                } else {
                    // Whether the declaration itself has a Modifier receiver - UAST might think the
                    // receiver on the node is different if it is inside another scope.
                    val hasModifierReceiver = when (val source = declaration?.sourcePsi) {
                        // Parsing a method defined in a class file
                        is ClsMethodImpl -> {
                            val receiverClassifier = source.toKmFunction()
                                ?.receiverParameterType?.classifier
                            receiverClassifier == KmClassifier.Class(Names.Ui.Modifier.kmClassName)
                        }
                        // Parsing a method defined in Kotlin source
                        is KtFunction -> {
                            val receiver = source.receiverTypeReference
                            (receiver.toUElement() as? UTypeReferenceExpression)
                                ?.getQualifiedName() == Names.Ui.Modifier.javaFqn
                        }
                        else -> false
                    }
                    if (hasModifierReceiver) {
                        isReceiverReferenced = true
                    }
                }
            }
            return isReceiverReferenced
        }

        /**
         * If `this` is explicitly referenced, no error.
         * TODO: consider checking for nested receivers, in case `this` refers to an
         * inner scope, and not the outer Modifier receiver
         */
        override fun visitThisExpression(node: UThisExpression): Boolean {
            isReceiverReferenced = true
            return isReceiverReferenced
        }
    })
    if (!isReceiverReferenced) {
        context.report(
            ModifierDeclarationDetector.ModifierFactoryUnreferencedReceiver,
            this,
            context.getNameLocation(this),
            "Modifier factory functions must use the receiver Modifier instance"
        )
    }
}

/**
 * @see [ModifierDeclarationDetector.ModifierFactoryReturnType]
 */
private fun UMethod.checkReturnType(context: JavaContext, returnType: PsiType) {
    fun report(lintFix: LintFix? = null) {
        context.report(
            ModifierFactoryReturnType,
            this,
            context.getNameLocation(this),
            "Modifier factory functions should have a return type of Modifier",
            lintFix
        )
    }

    if (returnType.canonicalText == Names.Ui.Modifier.javaFqn) return

    val source = sourcePsi
    if (source is KtCallableDeclaration && source.returnTypeString != null) {
        // Function declaration with an explicit return type, such as
        // `fun foo(): Modifier.element = Bar`. Replace the type with `Modifier`.
        report(
            LintFix.create()
                .replace()
                .name("Change return type to Modifier")
                .range(context.getLocation(this))
                .text(source.returnTypeString)
                .with(Names.Ui.Modifier.shortName)
                .autoFix()
                .build()
        )
        return
    }
    if (source is KtPropertyAccessor) {
        // Getter declaration with an explicit return type on the getter, such as
        // `val foo get(): Modifier.Element = Bar`. Replace the type with `Modifier`.
        val getterReturnType = source.returnTypeReference?.text

        if (getterReturnType != null) {
            report(
                LintFix.create()
                    .replace()
                    .name("Change return type to Modifier")
                    .range(context.getLocation(this))
                    .text(getterReturnType)
                    .with(Names.Ui.Modifier.shortName)
                    .autoFix()
                    .build()
            )
            return
        }
        // Getter declaration with an implicit return type from the property, such as
        // `val foo: Modifier.Element get() = Bar`. Replace the type with `Modifier`.
        val propertyType = source.property.returnTypeString

        if (propertyType != null) {
            report(
                LintFix.create()
                    .replace()
                    .name("Change return type to Modifier")
                    .range(context.getLocation(source.property))
                    .text(propertyType)
                    .with(Names.Ui.Modifier.shortName)
                    .autoFix()
                    .build()
            )
            return
        }
    }
    if (source is KtDeclarationWithBody) {
        // Declaration without an explicit return type, such as `fun foo() = Bar`
        // or val foo get() = Bar
        // Replace the `=` with `: Modifier =`
        report(
            LintFix.create()
                .replace()
                .name("Add explicit Modifier return type")
                .range(context.getLocation(this))
                .pattern("[ \\t\\n]+=")
                .with(": ${Names.Ui.Modifier.shortName} =")
                .autoFix()
                .build()
        )
        return
    }
}

/**
 * TODO: UMethod.returnTypeReference is not available in LINT_API_MIN, so instead use this with a
 * [KtCallableDeclaration].
 * See [org.jetbrains.uast.kotlin.declarations.KotlinUMethod.returnTypeReference] on newer UAST
 * versions.
 */
private val KtCallableDeclaration.returnTypeString: String?
    get() {
        return typeReference?.text
    }
