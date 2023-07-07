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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.Names
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.kotlin.KotlinUastResolveProviderService
import java.util.EnumSet

/**
 * [Detector] that checks `mutableStateOf` calls to warn if the type is a mutable collection, as
 * mutations to the mutable collection inside the state won't cause invalidations. Instead, it is
 * recommended to use mutableStateListOf / mutableStateMapOf for observable mutable collections, or
 * mutableStateOf with a read-only collection inside, and assigning a new instance when the data
 * changes.
 */
class MutableCollectionMutableStateDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(
        Names.Runtime.MutableStateOf.shortName
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Runtime.PackageName)) return

        val expression = node.sourcePsi as? KtExpression ?: return

        // PsiType will return the underlying JVM type for kotlin collections, so instead we need to
        // use the KotlinType to preserve the actual Kotlin type declared in source - that way we
        // can disambiguate between MutableList and the underlying java.util.List that it will be
        // converted to.
        val service = expression.project.getService(KotlinUastResolveProviderService::class.java)
        val bindingContext = service.getBindingContext(expression)
        val expressionType = bindingContext.getType(expression)

        // expressionType will be MutableState<Foo>, so unwrap the argument to get the type we care
        // about. We do this instead of looking at the inner expression type, to account for cases
        // such as mutableStateOf<List<Int>>(mutableListOf(1)) or
        // val foo: MutableState<List<Int>> = mutableStateOf(mutableListOf(1)) - the inner
        // expression type is mutable but because the type of the mutableStateOf expression is not,
        // we don't want to report a warning.
        val type = expressionType?.arguments?.firstOrNull()?.type ?: return

        if (type.isMutableCollection()) {
            context.report(
                MutableCollectionMutableState,
                node,
                context.getNameLocation(node),
                "Creating a MutableState object with a mutable collection type"
            )
        }
    }

    companion object {
        val MutableCollectionMutableState = Issue.create(
            "MutableCollectionMutableState",
            "Creating a MutableState object with a mutable collection type",
            "Writes to mutable collections inside a MutableState will not cause a " +
                "recomposition - only writes to the MutableState itself will. In most cases you " +
                "should either use a read-only collection (such as List or Map) and assign a new " +
                "instance to the MutableState when your data changes, or you can use " +
                "an snapshot-backed collection such as SnapshotStateList or SnapshotStateMap " +
                "which will correctly cause a recomposition when their contents are modified.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                MutableCollectionMutableStateDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

/**
 * Returns whether this type can be considered a mutable collection.
 * Returns true if this is, or this is a subclass of:
 *
 * - [kotlin.collections.MutableCollection]
 * - [kotlin.collections.MutableMap]
 *
 * If not, returns false if this is, or this is a subclass of:
 *
 * - [kotlin.collections.Collection]
 * - [kotlin.collections.Map]
 *
 * If not, returns true if this is, or this is a subclass of:
 * - [java.util.Collection]
 * - [java.util.Map]
 */
private fun KotlinType.isMutableCollection(): Boolean {
    // MutableCollection::class.qualifiedName == Collection::class.qualifiedName, so using hardcoded
    // strings instead
    val kotlinImmutableTypes = listOf(
        "kotlin.collections.Collection",
        "kotlin.collections.Map",
    )

    val kotlinMutableTypes = listOf(
        "kotlin.collections.MutableCollection",
        "kotlin.collections.MutableMap"
    )

    val javaMutableTypes = listOf(
        "java.util.Collection",
        "java.util.Map"
    )

    // Check `this`
    if (kotlinMutableTypes.any { it == fqn }) return true
    if (kotlinImmutableTypes.any { it == fqn }) return false
    if (javaMutableTypes.any { it == fqn }) return true

    // Check supertypes
    val supertypes = supertypes()
    if (supertypes.any { type -> kotlinMutableTypes.any { it == type.fqn } }) return true
    if (supertypes.any { type -> kotlinImmutableTypes.any { it == type.fqn } }) return false
    if (supertypes.any { type -> javaMutableTypes.any { it == type.fqn } }) return true

    return false
}

private val KotlinType.fqn: String? get() {
    val descriptor = constructor.declarationDescriptor ?: return null
    val packageName = descriptor.containingPackage()?.asString() ?: return null
    return packageName + "." + descriptor.name.asString()
}