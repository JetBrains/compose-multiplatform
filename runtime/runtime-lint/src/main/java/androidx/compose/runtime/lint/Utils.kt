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

package androidx.compose.runtime.lint

import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsMemberImpl
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.impl.compiled.ClsParameterImpl
import com.intellij.psi.util.ClassUtil
import kotlinx.metadata.Flag
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmFunction
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.annotations
import kotlinx.metadata.jvm.signature
import org.jetbrains.kotlin.lexer.KtTokens.INLINE_KEYWORD
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.kotlin.AbstractKotlinUVariable
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement

// TODO: KotlinUMethodWithFakeLightDelegate.hasAnnotation() returns null for some reason, so just
// look at the annotations directly
// TODO: annotations is deprecated but the replacement uAnnotations isn't available on the
// version of lint / uast we compile against
/**
 * Returns whether this method is @Composable or not
 */
@Suppress("DEPRECATION")
val UMethod.isComposable
    get() = annotations.any { it.qualifiedName == ComposableFqn }

/**
 * Returns whether this parameter's type is @Composable or not
 */
val UParameter.isComposable: Boolean
    get() = when (val source = sourcePsi) {
        // The parameter is defined in Kotlin source
        is KtParameter -> source.typeReference!!.isComposable
        // The parameter is in a class file. Currently type annotations aren't added to the
        // underlying type (https://youtrack.jetbrains.com/issue/KT-45307), so instead we use the
        // metadata annotation.
        is ClsParameterImpl -> {
            // Find the containing method, so we can get metadata from the containing class
            val containingMethod = getContainingUMethod()!!.sourcePsi as ClsMethodImpl
            val declarationContainer = containingMethod.getKmDeclarationContainer()
            val kmFunction = declarationContainer?.findKmFunctionForPsiMethod(containingMethod)

            val kmValueParameter = kmFunction?.valueParameters?.find {
                it.name == name
            }

            kmValueParameter?.type?.annotations?.find {
                it.className == KmComposableFqn
            } != null
        }
        // The parameter is in Java source / other, ignore
        else -> false
    }

/**
 * Returns whether this type reference is @Composable or not
 */
val KtTypeReference.isComposable: Boolean
    // This annotation should be available on the PsiType itself in 1.4.30+, but we are
    // currently on an older version of UAST / Kotlin embedded compiled
    // (https://youtrack.jetbrains.com/issue/KT-45244)
    get() = annotationEntries.any {
        (it.toUElement() as UAnnotation).qualifiedName == ComposableFqn
    }

/**
 * Returns whether this lambda expression is @Composable or not
 */
val ULambdaExpression.isComposable: Boolean
    get() {
        when (val lambdaParent = uastParent) {
            // Function call with a lambda parameter
            is KotlinUFunctionCallExpression -> {
                val parameter = lambdaParent.getParameterForArgument(this) ?: return false
                if (!(parameter.toUElement() as UParameter).isComposable) return false
            }
            // A local / non-local lambda variable
            is AbstractKotlinUVariable -> {
                val hasComposableAnnotationOnLambda = findAnnotation(ComposableFqn) != null
                val hasComposableAnnotationOnType =
                    (lambdaParent.typeReference?.sourcePsi as? KtTypeReference)
                        ?.isComposable == true

                if (!hasComposableAnnotationOnLambda && !hasComposableAnnotationOnType) return false
            }
            // This probably shouldn't be called, but safe return in case a new UAST type is added
            // in the future
            else -> return false
        }
        return true
    }

/**
 * @return whether the resolved declaration for this call expression is an inline function
 */
val KotlinUFunctionCallExpression.isDeclarationInline: Boolean
    get() {
        return when (val source = resolveToUElement()?.sourcePsi) {
            // Parsing a method defined in a class file
            is ClsMethodImpl -> {
                val declarationContainer = source.getKmDeclarationContainer()

                val flags = declarationContainer
                    ?.findKmFunctionForPsiMethod(source)?.flags ?: return false
                return Flag.Function.IS_INLINE(flags)
            }
            // Parsing a method defined in Kotlin source
            is KtNamedFunction -> {
                source.hasModifier(INLINE_KEYWORD)
            }
            // Parsing something else (such as a property) which cannot be inline
            else -> false
        }
    }

// TODO: https://youtrack.jetbrains.com/issue/KT-45310
// Currently there is no built in support for parsing kotlin metadata from kotlin class files, so
// we need to manually inspect the annotations and work with Cls* (compiled PSI).
/**
 * Returns the [KmDeclarationContainer] using the kotlin.Metadata annotation present on the
 * surrounding class. Returns null if there is no surrounding annotation (not parsing a Kotlin
 * class file), the annotation data is for an unsupported version of Kotlin, or if the metadata
 * represents a synthetic
 */
private fun ClsMemberImpl<*>.getKmDeclarationContainer(): KmDeclarationContainer? {
    val classKotlinMetadataAnnotation = containingClass?.annotations?.find {
        // hasQualifiedName() not available on the min version of Lint we compile against
        it.qualifiedName == KotlinMetadataFqn
    } ?: return null

    val metadata = KotlinClassMetadata.read(classKotlinMetadataAnnotation.toHeader())
        ?: return null

    return when (metadata) {
        is KotlinClassMetadata.Class -> metadata.toKmClass()
        is KotlinClassMetadata.FileFacade -> metadata.toKmPackage()
        is KotlinClassMetadata.SyntheticClass -> null
        is KotlinClassMetadata.MultiFileClassFacade -> null
        is KotlinClassMetadata.MultiFileClassPart -> metadata.toKmPackage()
        is KotlinClassMetadata.Unknown -> null
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

const val RuntimePackageName = "androidx.compose.runtime"

const val ComposableFqn = "$RuntimePackageName.Composable"
// kotlinx.metadata represents separators as `/` instead of `.`
val KmComposableFqn get() = ComposableFqn.replace(".", "/")

const val RememberShortName = "remember"

const val CoroutinePackageName = "kotlinx.coroutines"
const val AsyncShortName = "async"
const val LaunchShortName = "launch"

private const val KotlinMetadataFqn = "kotlin.Metadata"
