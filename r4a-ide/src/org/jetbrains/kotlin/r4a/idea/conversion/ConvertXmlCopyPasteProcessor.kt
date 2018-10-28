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
import com.intellij.psi.*
import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.conversion.copy.*
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.EmptyDocCommentConverter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.r4a.idea.conversion.XmlToKtxConverter.XmlNamespace
import org.jetbrains.kotlin.r4a.idea.editor.KtxEditorOptions
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.lang.IllegalStateException

class ConvertXmlCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertXmlCopyPasteProcessor::class.java)

    private class CopiedXmlCode(val fileText: String, val startOffsets: IntArray, val endOffsets: IntArray) : TextBlockTransferableData {
        override fun getFlavor() = DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor = DataFlavor(CopiedXmlCode::class.java, "class: ConvertXmlCopyPasteProcessor.CopiedXmlCode")
        }
    }

    private class CopiedText(val text: String) : TextBlockTransferableData {
        override fun getFlavor() = DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor = DataFlavor(CopiedText::class.java, "class: ConvertXmlCopyPasteProcessor.CopiedText")
        }
    }

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
            } else if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val text = content.getTransferData(DataFlavor.stringFlavor) as String
                return listOf(CopiedText(text))
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

        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? KtFile ?: return
        val transferableData = values.single()
        val (fileText, startOffsets, endOffsets) = when (transferableData) {
            is CopiedXmlCode -> Triple(transferableData.fileText, transferableData.startOffsets, transferableData.endOffsets)
            is CopiedText -> Triple(transferableData.text, intArrayOf(0), intArrayOf(transferableData.text.length))
            else -> throw IllegalStateException("Unsupported transferable data: $transferableData")
        }
        val xmlFile = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, fileText) as XmlFile
        val hasErrors = xmlFile.anyDescendantOfType<PsiErrorElement>()
        if (hasErrors) {
            if (transferableData is CopiedText) return
            // TODO(jdemeulenaere): Warn that result might be incorrect.
        }

        if (!confirmConversion(project)) {
            return
        }

        val ranges = startOffsets.mapIndexed { i, startOffset -> TextRange(startOffset, endOffsets[i]) }
        val elementAndTextList = ElementAndTextList().apply { ranges.forEach { this.collectElementsToConvert(xmlFile, it) } }

        // Convert PsiElement's to AST Element's.
        val conversionResult = StringBuilder()
        val imports = hashSetOf<FqName>()
        elementAndTextList.process(object : ElementsAndTextsProcessor {
            override fun processElement(element: PsiElement) {
                // TODO(jdemeulenaere): Better comment converter.
                val codeBuilder = CodeBuilder(null, EmptyDocCommentConverter)
                codeBuilder.append(XmlToKtxConverter.convertElement(element))
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