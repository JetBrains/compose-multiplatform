/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.google.common.base.CaseFormat
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.name.FqName

object XmlToKtxConverter {
    private val NAMESPACE_PREFIX = "xmlns"

    enum class XmlNamespace(val uri: String) {
        ANDROID("http://schemas.android.com/apk/res/android"),
        ANDROID_TOOLS("http://schemas.android.com/tools"),
        ANDROID_APP("http://schemas.android.com/apk/res-auto");

        companion object {
            val uriToNamespace = XmlNamespace.values().associateBy { it.uri }
        }
    }

    fun namespacePrefixes(file: XmlFile): Map<XmlNamespace, String> {
        return file.rootTag?.attributes
            ?.filter { it.namespacePrefix == NAMESPACE_PREFIX && it.value != null }
            ?.mapNotNull { attr -> XmlNamespace.uriToNamespace[attr.value]?.let { Pair(it, attr.localName) } }
            ?.toMap() ?: emptyMap()
    }

    fun convertElement(
        element: PsiElement,
        namespacePrefixes: Map<XmlNamespace, String>
    ): Element = when (element) {
        is XmlFile -> convertFile(element, namespacePrefixes)
        is XmlTag -> convertTag(element, namespacePrefixes)
        is XmlAttribute -> convertAttribute(element)
        else -> DummyStringExpression(element.text)
    }

    fun convertFile(element: XmlFile, namespacePrefixes: Map<XmlNamespace, String>): Element {
        return element.rootTag?.let {
            convertTag(
                it,
                namespacePrefixes
            )
        } ?: DummyStringExpression("")
    }

    fun formatCode(file: PsiFile, range: RangeMarker? = null) {
        runWriteAction {
            val codeStyleManager = CodeStyleManager.getInstance(file.project)
            if (range != null && range.isValid) {
                codeStyleManager.reformatRange(file, range.startOffset, range.endOffset)
            } else {
                codeStyleManager.reformat(file)
            }
        }
    }

    private fun convertTag(
        tag: XmlTag,
        namespacePrefixes: Map<XmlNamespace, String>
    ): KtxElement {
        val (import, tagName) = getTagImportAndName(tag)
        val identifier = Identifier(tagName, isNullable = false, quotingNeeded = false, imports = listOf(import))
        val attributes = tag.attributes.filter {
            shouldConvertAttribute(
                it,
                namespacePrefixes
            )
        }.map(XmlToKtxConverter::convertAttribute)
        val body = tag.subTags.map { convertTag(it, namespacePrefixes) }
        return KtxElement(identifier, attributes, body)
    }

    private fun getTagImportAndName(tag: XmlTag): Pair<FqName, String> {
        val fullName = tag.name
        val dotIndex = fullName.lastIndexOf('.')
        if (dotIndex != -1) {
            // TODO(jdemeulenaere): Check that import doesn't conflict with another (existing or future) import (in which case use the fully qualified name).
            return FqName(fullName) to fullName.substring(dotIndex + 1)
        }

        // TODO(jdemeulenaere): Not sure this default package is always correct.
        return FqName("android.widget.$fullName") to fullName
    }

    private fun shouldConvertAttribute(attribute: XmlAttribute, namespacePrefixes: Map<XmlNamespace, String>): Boolean {
        return attribute.namespacePrefix != NAMESPACE_PREFIX && attribute.namespacePrefix != namespacePrefixes[XmlNamespace.ANDROID_TOOLS]
    }

    private fun convertAttribute(attribute: XmlAttribute): KtxAttribute {
        // TODO(jdemeulenaere): Improve attribute mapping.
        // TODO(jdemeulenaere): Convert string attribute value to correct type.
        val name = attribute.localName // Strips away the namespace.
        val camelCaseName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name)
        // TODO(jdemeulenaere): Better handle missing value.
        val value = attribute.value ?: ""
        val escapedValue = value
            .replace("\\", "\\\\") // Replaces all \ by \\
            .replace("\"", "\\\"") // Replaces all " by \"
            .replace("\$", "\\\$") // Replaces all $ by \$
        return KtxAttribute(camelCaseName, LiteralExpression("\"$escapedValue\""))
    }
}