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

import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.util.ClassUtil
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmFunction
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature

/**
 * @return the corresponding [KmFunction] for this [PsiMethod], or `null` if there is no
 * corresponding [KmFunction]. This method is only meaningful if this [PsiMethod] represents a
 * method defined in bytecode (most often a [ClsMethodImpl]).
 */
fun PsiMethod.toKmFunction(): KmFunction? =
    containingClass!!.getKmDeclarationContainer()?.findKmFunctionForPsiMethod(this)

// TODO: https://youtrack.jetbrains.com/issue/KT-45310
// Currently there is no built in support for parsing kotlin metadata from kotlin class files, so
// we need to manually inspect the annotations and work with Cls* (compiled PSI).
/**
 * Returns the [KmDeclarationContainer] using the kotlin.Metadata annotation present on this
 * [PsiClass]. Returns null if there is no annotation (not parsing a Kotlin
 * class file), the annotation data is for an unsupported version of Kotlin, or if the metadata
 * represents a synthetic class.
 */
private fun PsiClass.getKmDeclarationContainer(): KmDeclarationContainer? {
    val classKotlinMetadataAnnotation = annotations.find {
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
    val data1 = attributes["d1"]?.parseStringArray()
    val data2 = attributes["d2"]?.parseStringArray()
    val extraString = attributes["xs"]?.parseString()
    val packageName = attributes["pn"]?.parseString()
    val extraInt = attributes["xi"]?.parseInt()

    return KotlinClassHeader(
        kind,
        metadataVersion,
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
    // Strip any mangled part of the name in case of value / inline classes
    val expectedName = method.name.substringBefore("-")
    val expectedSignature = ClassUtil.getAsmMethodSignature(method)
    // Since Kotlin 1.6 PSI updates, in some cases what used to be `void` return types are converted
    // to `kotlin.Unit`, even though in the actual metadata they are still void. Try to match those
    // cases as well
    val unitReturnTypeSuffix = "Lkotlin/Unit;"
    val expectedSignatureConvertedFromUnitToVoid = if (
        expectedSignature.endsWith(unitReturnTypeSuffix)
    ) {
        expectedSignature.substringBeforeLast(unitReturnTypeSuffix) + "V"
    } else {
        expectedSignature
    }

    return functions.find {
        it.name == expectedName && (
            it.signature?.desc == expectedSignature ||
                it.signature?.desc == expectedSignatureConvertedFromUnitToVoid
        )
    }
}

private const val KotlinMetadataFqn = "kotlin.Metadata"
