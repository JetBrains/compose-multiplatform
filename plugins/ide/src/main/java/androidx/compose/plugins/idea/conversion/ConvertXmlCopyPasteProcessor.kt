/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.plugins.idea.conversion

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.conversion.copy.CopiedKotlinCode
import org.jetbrains.kotlin.idea.conversion.copy.ElementAndTextList
import org.jetbrains.kotlin.idea.conversion.copy.ElementsAndTextsProcessor
import org.jetbrains.kotlin.idea.conversion.copy.end
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.conversion.copy.start
import org.jetbrains.kotlin.idea.j2k.EmptyDocCommentConverter
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import androidx.compose.plugins.kotlin.ComposeUtils
import androidx.compose.plugins.idea.editor.KtxEditorOptions
import androidx.compose.plugins.idea.parentOfType
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class ConvertXmlCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertXmlCopyPasteProcessor::class.java)

    private class CopiedXmlCode(
        val fileText: String,
        val startOffsets: IntArray,
        val endOffsets: IntArray
    ) : TextBlockTransferableData {
        override fun getFlavor() =
            DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor = DataFlavor(
                CopiedXmlCode::class.java,
                "class: ConvertXmlCopyPasteProcessor.CopiedXmlCode"
            )
        }
    }

    private class CopiedText(val text: String) : TextBlockTransferableData {
        override fun getFlavor() =
            DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor = DataFlavor(
                CopiedText::class.java,
                "class: ConvertXmlCopyPasteProcessor.CopiedText"
            )
        }
    }

    override fun collectTransferableData(
        file: PsiFile,
        editor: Editor,
        startOffsets: IntArray,
        endOffsets: IntArray
    ): List<TextBlockTransferableData> {
        if (file !is XmlFile) return emptyList()
        return listOf(
            CopiedXmlCode(
                file.text,
                startOffsets,
                endOffsets
            )
        )
    }

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            when {
                // Do not try to convert text copied from Kotlin file.
                content.isDataFlavorSupported(CopiedKotlinCode.DATA_FLAVOR) -> return emptyList()
                content.isDataFlavorSupported(CopiedXmlCode.DATA_FLAVOR) -> {
                    return listOf(content.getTransferData(CopiedXmlCode.DATA_FLAVOR)
                            as TextBlockTransferableData)
                }
                content.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                    val text = content.getTransferData(DataFlavor.stringFlavor) as String
                    return listOf(
                        CopiedText(
                            text
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            LOG.error(e)
        }
        return emptyList()
    }

    override fun processTransferableData(
        project: Project,
        editor: Editor,
        bounds: RangeMarker,
        caretOffset: Int,
        indented: Ref<Boolean>,
        values: List<TextBlockTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb) return

        val editorOptions = KtxEditorOptions.getInstance()
        if (!editorOptions.enableXmlToKtxConversion) return

        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as?
                KtFile ?: return
        val transferableData = values.single()
        var (fileText, startOffsets, endOffsets) = when (transferableData) {
            is CopiedXmlCode ->
                Triple(
                    transferableData.fileText,
                    transferableData.startOffsets,
                    transferableData.endOffsets
                )
            is CopiedText ->
                Triple(
                    transferableData.text,
                    intArrayOf(0),
                    intArrayOf(transferableData.text.length)
                )
            else ->
                throw IllegalStateException("Unsupported transferable data: $transferableData")
        }
        var xmlFile = createXmlFile(project, fileText)

        if (transferableData is CopiedText) {
            if (hasErrors(xmlFile)) {
                // Try to parse as attributes if we are inside a KTX tag.
                val closestKtxTag =
                    targetFile.findElementAt(caretOffset)?.parentOfType<KtxElement>() ?: return
                val tagName =
                    closestKtxTag.qualifiedTagName?.text ?: closestKtxTag.simpleTagName?.text
                    ?: return

                // Check that the caret is inside that tag attributes list.
                val bracketsElements = closestKtxTag.bracketsElements
                val firstLTPosition = bracketsElements
                    .filter { it is TreeElement && it.elementType == KtTokens.LT }
                    .map { it.startOffset }
                    .min() ?: return
                val firstGTPosition = bracketsElements
                    .filter { it is TreeElement && it.elementType == KtTokens.GT }
                    .map { it.startOffset }
                    .min() ?: return

                if (caretOffset < firstLTPosition || caretOffset > firstGTPosition) {
                    return
                }

                // Create dummy XML tag and file.
                val filePrefix = "<$tagName "
                val newStartOffset = filePrefix.length
                startOffsets = intArrayOf(newStartOffset)
                endOffsets = intArrayOf(newStartOffset + fileText.length)
                xmlFile = createXmlFile(project, "$filePrefix$fileText />")

                if (hasErrors(xmlFile)) {
                    return
                }
            }

            // Make sure we don't try to convert copied KTX code.
            if (!hasAttributeWithNamespace(xmlFile)) {
                return
            }
        }

        val ranges = startOffsets.mapIndexed { i, startOffset ->
            TextRange(startOffset, endOffsets[i])
        }
        val elementAndTextList = ElementAndTextList().apply {
            ranges.forEach { this.collectElementsToConvert(xmlFile, it) }
        }

        // Convert PsiElement's to AST Element's.
        val conversionResult = StringBuilder()
        val imports = hashSetOf<FqName>()
        val converter = XmlToKtxConverter(targetFile)
        var skipConversion = true
        elementAndTextList.process(object : ElementsAndTextsProcessor {
            override fun processElement(element: PsiElement) {
                // TODO(jdemeulenaere): Better comment converter.
                val codeBuilder = CodeBuilder(null, EmptyDocCommentConverter)
                val resultKtx = converter.convertElement(element)
                if (resultKtx != null) {
                    skipConversion = false
                    codeBuilder.append(resultKtx)
                    imports.addAll(codeBuilder.importsToAdd)
                    conversionResult.append(codeBuilder.resultText)
                } else {
                    conversionResult.append(element.text)
                }
            }

            override fun processText(string: String) {
                conversionResult.append(string)
            }
        })

        if (skipConversion || !confirmConversion(project)) {
            return
        }

        // Replace text.
        val resultKtx = conversionResult.toString()
        val enclosingCallable = targetFile.findElementAt(bounds.startOffset)?.let {
            enclosingCallable(it)
        }
        val conversionResultText = if (enclosingCallable != null) {
            resultKtx
        } else {
            // TODO(jdemeulenaere): Find better function name. If we are pasting in a class that has the same name as the file (common
            // case), we will clash with the constructor. Maybe override invoke operator if it's not already overridden in that class or
            // its subclasses ?
            val functionName = targetFile.name.takeWhile { it != '.' }
            createFunctionalComponent(
                functionName,
                resultKtx,
                imports
            )
        }
        val rangeAfterReplace = replaceText(editor, conversionResultText, bounds)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        // Suggest to add @Composable annotation and/or androidx.compose.* import if necessary.
        if (editorOptions.enableAddComposableAnnotation) {
            val fixes = arrayListOf<() -> Unit>()

            // Add @Composable annotation.
            if (enclosingCallable != null &&
                // TODO(jdemeulenaere): For some reason, checking for annotations on lambdas doesn't work. Fix that and remove this check.
                enclosingCallable is KtNamedFunction) {
                val annotationFqName = ComposeUtils.composeFqName("Composable")
                if (enclosingCallable.findAnnotation(annotationFqName) == null) {
                    fixes.add {
                        runWriteAction {
                            enclosingCallable.addAnnotation(annotationFqName)
                        }
                    }
                }
            }

            // Add androidx.compose.* import.
            val composeStarFqName = ComposeUtils.composeFqName("*")
            if (targetFile.importDirectives.none { it.importedFqName == composeStarFqName }) {
                fixes.add { addComposeStarImport(targetFile) }
            }

            if (fixes.isNotEmpty()) {
                // Ask for approval if necessary.
                val shouldApplyFixes = if (editorOptions.donTShowAddComposableAnnotationDialog) {
                    true
                } else {
                    val dialog =
                        KtxAddComposableAnnotationDialog(
                            project
                        )
                    dialog.show()
                    dialog.isOK
                }

                // Apply fixes.
                if (shouldApplyFixes) {
                    fixes.forEach { it() }
                }
            }
        }

        // TODO(jdemeulenaere): If enclosingCallable == null (i.e. if we generated the enclosing function), put the cursor at the function
        // name position and select it to easily change it right after pasting.

        // Import classes.
        if (imports.isNotEmpty()) {
            runWriteAction {
                imports.forEach { fqName ->
                    targetFile.resolveImportReference(fqName).firstOrNull()?.let {
                        ImportInsertHelper.getInstance(project).importDescriptor(targetFile, it)
                    }
                }
            }

            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }

        // Format code.
        XmlToKtxConverter.formatCode(
            targetFile,
            rangeAfterReplace
        )
        return
    }

    private fun createXmlFile(project: Project, fileText: String): XmlFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            XMLLanguage.INSTANCE, fileText
        ) as XmlFile
    }

    private fun hasErrors(xmlFile: XmlFile) = xmlFile.anyDescendantOfType<PsiErrorElement>()

    private fun enclosingCallable(element: PsiElement): KtCallableDeclaration? {
        var current = element
        while (current !is KtFile) {
            if (current is KtCallableDeclaration) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun hasAttributeWithNamespace(file: XmlFile): Boolean {
        val rootTag = file.rootTag ?: return false
        return hasAttributeWithNamespace(rootTag)
    }

    private fun hasAttributeWithNamespace(tag: XmlTag): Boolean {
        for (attribute in tag.attributes) {
            if (attribute.namespacePrefix.isNotEmpty()) {
                return true
            }
        }

        for (subTag in tag.subTags) {
            if (hasAttributeWithNamespace(subTag)) {
                return true
            }
        }

        return false
    }

    private fun replaceText(
        editor: Editor,
        conversionResultText: String,
        bounds: RangeMarker
    ): RangeMarker {
        return runWriteAction {
            val startOffset = bounds.startOffset
            editor.document.replaceString(startOffset, bounds.endOffset, conversionResultText)

            val endOffset = startOffset + conversionResultText.length
            editor.caretModel.moveToOffset(endOffset)
            editor.document.createRangeMarker(startOffset, endOffset).apply {
                isGreedyToLeft = true
                isGreedyToRight = true
            }
        }
    }

    private fun ElementAndTextList.collectElementsToConvert(file: PsiFile, range: TextRange) {
        val elements = file.elementsInRange(range)
        val fileText = file.text
        if (elements.isEmpty()) {
            add(fileText.substring(range.start, range.end))
        } else {
            add(fileText.substring(range.start, elements.first().range.start))
            this += elements
            add(fileText.substring(elements.last().range.end, range.end))
        }
    }

    private fun confirmConversion(project: Project): Boolean {
        if (KtxEditorOptions.getInstance().donTShowKtxConversionDialog) return true

        val dialog = KtxPasteFromXmlDialog(project)
        dialog.show()
        return dialog.isOK
    }
}