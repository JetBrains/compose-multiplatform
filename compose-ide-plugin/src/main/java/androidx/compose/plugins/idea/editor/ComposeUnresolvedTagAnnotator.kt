package androidx.compose.plugins.idea.editor

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingUtil

/**
 * This class adds a red highlight to tag names in KTX elements that are unresolved. This could be done
 * by default by just using the `UNRESOLVED_REFERENCE` diagnostic, but we have decided not to use that
 * diagnostic in this case because the "Add Import" quick fix that it causes breaks in the case of
 * Open/Close KTX tags. Instead, we use our own `UNRESOLVED_TAG` diagnostic, which we register our own
 * quick fix for, and we also add the red highlight here.
 */
class ComposeUnresolvedTagAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val ktFile = element.containingFile as? KtFile ?: return

        if (!KotlinHighlightingUtil.shouldHighlight(ktFile)) return

        val analysisResult = ktFile.analyzeWithAllCompilerChecks()
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }

        val bindingContext = analysisResult.bindingContext
        val diagnostics = bindingContext.diagnostics

        element.accept(object : KtVisitorVoid() {
            override fun visitKtxElement(element: KtxElement) {
                element.simpleTagName?.let {
                    if (diagnostics.forElement(it).any {
                            it.realDiagnosticFactory == ComposeErrors.UNRESOLVED_TAG
                        }) {
                        holder.createErrorAnnotation(it, null).apply {
                            highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                        }
                    }
                }
                element.simpleClosingTagName?.let {
                    if (diagnostics.forElement(it).any {
                            it.realDiagnosticFactory == ComposeErrors.UNRESOLVED_TAG
                        }) {
                        holder.createErrorAnnotation(it, null).apply {
                            highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                        }
                    }
                }
            }
        })
    }
}

private val Diagnostic.realDiagnosticFactory: DiagnosticFactory<*>
    get() =
        when (factory) {
            Errors.PLUGIN_ERROR -> Errors.PLUGIN_ERROR.cast(this).a.factory
            Errors.PLUGIN_WARNING -> Errors.PLUGIN_WARNING.cast(this).a.factory
            Errors.PLUGIN_INFO -> Errors.PLUGIN_INFO.cast(this).a.factory
            else -> factory
        }