/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.conversion

import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.EmptyDocCommentConverter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class XmlToComponentAction : AnAction() {
    companion object {
        fun convertFiles(xmlFiles: List<XmlFile>) {
            if (xmlFiles.isEmpty()) {
                return
            }

            // TODO(jdemeulenaere): Check if there are syntax errors in any file, in which case either abort or warn that result might be incorrect.
            val newContentsAndImports = xmlFiles.mapNotNull { xmlFile ->
                val (content, imports) = convertFile(xmlFile)
                Triple(xmlFile, content, imports)
            }

            val project = xmlFiles[0].project
            project.executeWriteCommand("Convert XML Files to Component") {
                // Replace and rename files.
                val virtualFilesAndImports = newContentsAndImports.mapNotNull { (xmlFile, content, imports) ->
                    replaceAndRename(xmlFile, content)?.let { Pair(it, imports) }
                }
                PsiDocumentManager.getInstance(project).commitAllDocuments()

                virtualFilesAndImports.forEach { (file, imports) ->
                    val ktFile = file.toPsiFile(project) as? KtFile ?: return@forEach

                    // Add imports.
                    addImports(project, ktFile, imports)

                    // Format code.
                    XmlToKtxConverter.formatCode(ktFile)
                }

                // Open first converted file.
                virtualFilesAndImports.singleOrNull()?.let { (file, _) ->
                    FileEditorManager.getInstance(project).openFile(file, true)
                }
            }
        }

        private fun convertFile(file: XmlFile): Pair<String, Collection<FqName>> {
            // Generate class code.
            // TODO(jdemeulenaere): Better comment converter.
            val codeBuilder = CodeBuilder(null, EmptyDocCommentConverter)
            val convertedXml = XmlToKtxConverter.convertFile(
                file
            )
            codeBuilder.append(convertedXml)
            val className = className(file)
            val fileContent = createComponentClass(className, codeBuilder.resultText)
            // TODO(jdemeulenaere): For some reason, adding imports here on a dummy file to directly return the final content didn't work...
            // Hence I add them later once the file actually exists.
            return fileContent to codeBuilder.importsToAdd
        }

        private fun className(file: XmlFile): String {
            // TODO(jdemeulenaere): Ask the user which class name to use (and pre-fill name with this algorithm) if converting only one file.
            var className = file.name

            // Remove extension.
            val dotIndex = className.lastIndexOf('.')
            if (dotIndex != -1) {
                className = className.substring(0, dotIndex)
            }

            // Remove special characters.
            className = className.replace(Regex("[^A-Za-z0-9_]"), "")

            // Convert files named activity_* to *Component.
            if (className.startsWith("activity_")) {
                className = className.substring("activity_".length) + "_component"
            }

            // Convert case.
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, className)
        }

        private fun createComponentClass(name: String, composeBody: String): String {
            // TODO(jdemeulenaere): Infer package from file location/siblings/parents.
            // We don't use org.jetbrains.kotlin.j2k.ast.Class here because adding a function with a body inside a Class requires using a j2k
            // Converter class.
            return """
            |import com.google.r4a.Component
            |
            |class $name : Component() {
            |    override fun compose() {
            |        $composeBody
            |    }
            |}""".trimMargin()
        }

        private fun replaceAndRename(file: XmlFile, content: String): VirtualFile? {
            // Replace content.
            val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return null
            document.replaceString(0, document.textLength, content)
            FileDocumentManager.getInstance().saveDocument(document)

            // Rename file.
            // TODO(jdemeulenaere): If we are converting only one file, ask the user in which folder he wants to move that file.
            // TODO(jdemeulenaere): Handle scratch files (change language mapping).
            val virtualFile = file.virtualFile
            val ioFile = File(virtualFile.path.replace('/', File.separatorChar))
            val className = className(file)
            var kotlinFileName = "$className.kt"
            var i = 1
            while (true) {
                if (!ioFile.resolveSibling(kotlinFileName).exists()) break
                kotlinFileName = "$className${i++}.kt"
            }
            virtualFile.rename(this, kotlinFileName)
            return virtualFile
        }

        private fun addImports(project: Project, ktFile: KtFile, imports: Collection<FqName>) {
            runWriteAction {
                imports.forEach { fqName ->
                    ktFile.resolveImportReference(fqName).firstOrNull()?.let {
                        ImportInsertHelper.getInstance(project).importDescriptor(ktFile, it)
                    }
                }
            }
        }

        private fun isAnyXmlFileSelected(project: Project, files: Array<VirtualFile>): Boolean {
            val manager = PsiManager.getInstance(project)

            if (files.any { manager.findFile(it) is XmlFile && it.isWritable }) return true
            return files.any { it.isDirectory && isAnyXmlFileSelected(project, it.children) }
        }

        private fun selectedXmlFiles(event: AnActionEvent): Sequence<XmlFile> {
            val virtualFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
            val project = event.project ?: return sequenceOf()
            return allXmlFiles(virtualFiles, project)
        }

        private fun allXmlFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<XmlFile> {
            val manager = PsiManager.getInstance(project)
            return allFiles(filesOrDirs)
                .asSequence()
                .mapNotNull { manager.findFile(it) as? XmlFile }
        }

        private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
            val result = ArrayList<VirtualFile>()
            for (file in filesOrDirs) {
                VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        result.add(file)
                        return true
                    }
                })
            }
            return result
        }

    }

    override fun actionPerformed(e: AnActionEvent) {
        val xmlFiles = selectedXmlFiles(e).filter { it.isWritable }.toList()
        convertFiles(xmlFiles)
    }

    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = e.project ?: return
        e.presentation.isVisible = isAnyXmlFileSelected(project, virtualFiles)
    }
}