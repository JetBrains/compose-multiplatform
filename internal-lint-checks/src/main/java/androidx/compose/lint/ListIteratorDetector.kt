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

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.InheritanceUtil
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmFunction
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement

/**
 * Lint [Detector] to prevent allocating Iterators when iterating on a [List]. Instead of using
 * `for (e in list)` or `list.forEach {}`, more efficient iteration methods should be used, such as
 * `for (i in list.indices) { list[i]... }` or `list.fastForEach`.
 */
class ListIteratorDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(
        UForEachExpression::class.java,
        UCallExpression::class.java
    )

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitForEachExpression(node: UForEachExpression) {
            // Type of the variable we are iterating on, i.e the type of `b` in `for (a in b)`
            val iteratedValueType = node.iteratedValue.getExpressionType()
            // We are iterating on a List
            if (InheritanceUtil.isInheritor(iteratedValueType, JavaListFqn)) {
                // Find the `in` keyword to use as location
                val inKeyword = (node.sourcePsi as? KtForExpression)?.inKeyword
                val location = if (inKeyword == null) {
                    context.getNameLocation(node)
                } else {
                    context.getNameLocation(inKeyword)
                }
                context.report(
                    ISSUE,
                    node,
                    location,
                    "Creating an unnecessary Iterator to iterate through a List"
                )
            }
        }

        override fun visitCallExpression(node: UCallExpression) {
            val receiverType = node.receiverType

            // We are calling a method on a `List` type
            if (receiverType != null &&
                InheritanceUtil.isInheritor(node.receiverType, JavaListFqn)
            ) {
                when (val method = node.resolveToUElement()?.sourcePsi) {
                    // Parsing a class file
                    is ClsMethodImpl -> {
                        method.checkForIterableReceiver(node)
                    }
                    // Parsing Kotlin source
                    is KtNamedFunction -> {
                        method.checkForIterableReceiver(node)
                    }
                }
            }
        }

        private fun ClsMethodImpl.checkForIterableReceiver(node: UCallExpression) {
            val classKotlinMetadataAnnotation = containingClass?.annotations?.find {
                it.hasQualifiedName(KotlinMetadataFqn)
            } ?: return

            val metadata = KotlinClassMetadata.read(classKotlinMetadataAnnotation.toHeader())
                ?: return

            // Since we are visiting a function, the null branches shouldn't be called
            val kmPackage: KmDeclarationContainer = when (metadata) {
                is KotlinClassMetadata.Class -> metadata.toKmClass()
                is KotlinClassMetadata.FileFacade -> metadata.toKmPackage()
                is KotlinClassMetadata.SyntheticClass -> null
                is KotlinClassMetadata.MultiFileClassFacade -> null
                is KotlinClassMetadata.MultiFileClassPart -> metadata.toKmPackage()
                is KotlinClassMetadata.Unknown -> null
            }!!

            val kmFunction = kmPackage.findKmFunctionForPsiMethod(this)

            kmFunction?.let {
                if (it.hasIterableReceiver) {
                    context.report(
                        ISSUE,
                        node,
                        context.getNameLocation(node),
                        "Creating an unnecessary Iterator to iterate through a List"
                    )
                }
            }
        }

        private fun KtNamedFunction.checkForIterableReceiver(node: UCallExpression) {
            val receiver = receiverTypeReference
            // If there is no receiver, or the receiver isn't an Iterable, ignore
            if ((receiver.toUElement() as? UTypeReferenceExpression)
                ?.getQualifiedName() != JavaIterableFQN
            ) return

            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                "Creating an unnecessary Iterator to iterate through a List"
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "ListIterator",
            "Creating an unnecessary Iterator to iterate through a List",
            "Iterable<T> extension methods and using `for (a in list)` will create an " +
                "Iterator object - in hot code paths this can cause a lot of extra allocations " +
                "which is something we want to avoid. Instead, use a method that doesn't " +
                "allocate, such as `fastForEach`, or use `for (a in list.indices)` as iterating " +
                "through an `IntRange` does not allocate an Iterator, and becomes just a simple " +
                "for loop.",
            Category.PERFORMANCE, 5, Severity.ERROR,
            Implementation(
                ListIteratorDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}

/**
 * Returns a [KotlinClassHeader] by parsing the attributes of this @kotlin.Metadata annotation.
 *
 * See: https://github.com/udalov/kotlinx-metadata-examples/blob/master/src/main/java
 * /examples/FindKotlinGeneratedMethods.java
 */
private fun PsiAnnotation.toHeader(): KotlinClassHeader {
    val attributes = attributes.associate { it.attributeName to it.attributeValue }

    fun JvmAnnotationAttributeValue.parseString(): String =
        (this as JvmAnnotationConstantValue).constantValue as String

    fun JvmAnnotationAttributeValue.parseInt(): Int =
        (this as JvmAnnotationConstantValue).constantValue as Int

    fun JvmAnnotationAttributeValue.parseStringArray(): Array<String> =
        (this as JvmAnnotationArrayValue).values.map {
            it.parseString()
        }.toTypedArray()

    fun JvmAnnotationAttributeValue.parseIntArray(): IntArray =
        (this as JvmAnnotationArrayValue).values.map {
            it.parseInt()
        }.toTypedArray().toIntArray()

    val kind = attributes["k"]?.parseInt()
    val metadataVersion = attributes["mv"]?.parseIntArray()
    val bytecodeVersion = attributes["bv"]?.parseIntArray()
    val data1 = attributes["d1"]?.parseStringArray()
    val data2 = attributes["d2"]?.parseStringArray()
    val extraString = attributes["xs"]?.parseString()
    val packageName = attributes["pn"]?.parseString()
    val extraInt = attributes["xi"]?.parseInt()

    return KotlinClassHeader(
        kind,
        metadataVersion,
        bytecodeVersion,
        data1,
        data2,
        extraString,
        packageName,
        extraInt
    )
}

/**
 * @return the corresponding [KmFunction] in [this] for the given [method], matching by name and
 * signature.
 */
private fun KmDeclarationContainer.findKmFunctionForPsiMethod(method: PsiMethod): KmFunction? {
    val expectedName = method.name
    val expectedSignature = ClassUtil.getAsmMethodSignature(method)

    return functions.find {
        it.name == expectedName && it.signature?.desc == expectedSignature
    }
}

/**
 * @return true if this function is an extension function on Iterable
 */
private val KmFunction.hasIterableReceiver: Boolean
    get() = receiverParameterType?.classifier == IterableClassifier

private const val KotlinMetadataFqn = "kotlin.Metadata"
// Kotlin collections on JVM are just the underlying Java collections
private const val JavaListFqn = "java.util.List"
private const val JavaIterableFQN = "java.lang.Iterable"
private val IterableClassifier = KmClassifier.Class("kotlin/collections/Iterable")
