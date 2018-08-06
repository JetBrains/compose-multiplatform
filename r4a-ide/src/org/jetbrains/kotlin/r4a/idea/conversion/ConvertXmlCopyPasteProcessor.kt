/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

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
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.conversion.copy.*
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.EmptyDocCommentConverter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.r4a.idea.conversion.XmlToKtxConverter.XmlNamespace
import org.jetbrains.kotlin.r4a.idea.editor.KtxEditorOptions
import java.awt.datatransfer.Transferable

class ConvertXmlCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertXmlCopyPasteProcessor::class.java)

    override fun collectTransferableData(
        file: PsiFile,
        editor: Editor,
        startOffsets: IntArray,
        endOffsets: IntArray
    ): List<TextBlockTransferableData> {
        if (file !is XmlFile) return emptyList()
        return listOf(CopiedXmlCode(file.text, startOffsets, endOffsets))
    }

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(CopiedXmlCode.DATA_FLAVOR)) {
                return listOf(content.getTransferData(CopiedXmlCode.DATA_FLAVOR) as TextBlockTransferableData)
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
        if (!KtxEditorOptions.getInstance().enableXmlToKtxConversion) return

        // TODO(jdemeulenaere): Handle pasting from anywhere (not only from XML files in the editor).
        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
        val copiedCode = values.single() as CopiedXmlCode
        val xmlFile = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, copiedCode.fileText) as XmlFile
        // TODO(jdemeulenaere): Check if there are syntax errors in XML file, in which case either abort or warn that result might be incorrect.
        val namespacePrefixes = XmlToKtxConverter.namespacePrefixes(xmlFile)
        // TODO(jdemeulenaere): Improve this. Maybe we don't want to restrict only to android XML files.
        if (!namespacePrefixes.contains(XmlNamespace.ANDROID)) {
            return
        }

        if (!confirmConversion(project)) {
            return
        }

        val ranges = copiedCode.startOffsets.mapIndexed { i, startOffset -> TextRange(startOffset, copiedCode.endOffsets[i]) }
        val elementAndTextList = ElementAndTextList().apply { ranges.forEach { this.collectElementsToConvert(xmlFile, it) } }

        // Convert PsiElement's to AST Element's.
        val conversionResult = StringBuilder()
        val imports = hashSetOf<FqName>()
        elementAndTextList.process(object : ElementsAndTextsProcessor {
            override fun processElement(element: PsiElement) {
                // TODO(jdemeulenaere): Better comment converter.
                val codeBuilder = CodeBuilder(null, EmptyDocCommentConverter)
                codeBuilder.append(XmlToKtxConverter.convertElement(element, namespacePrefixes))
                imports.addAll(codeBuilder.importsToAdd)
                conversionResult.append(codeBuilder.resultText)
            }

            override fun processText(string: String) {
                conversionResult.append(string)
            }
        })

        // Replace text.
        val conversionResultText = conversionResult.toString()
        val rangeAfterReplace = replaceText(editor, conversionResultText, bounds)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

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
        XmlToKtxConverter.formatCode(targetFile, rangeAfterReplace)
        return
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