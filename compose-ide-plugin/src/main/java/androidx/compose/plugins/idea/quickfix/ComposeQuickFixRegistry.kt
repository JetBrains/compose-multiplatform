package androidx.compose.plugins.idea.quickfix

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import androidx.compose.plugins.kotlin.analysis.ComposeErrors

class ComposeQuickFixRegistry : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {

        // Provide fixes to annotate things that need to be @Composable when they aren't
        quickFixes.register(
            ComposeErrors.KTX_IN_NON_COMPOSABLE,
            AnnotateWithComposableQuickFix
        )
        quickFixes.register(
            ComposeErrors.NON_COMPOSABLE_INVOCATION,
            AnnotateTargetWithComposableQuickFix
        )

        // Provide fixes for calling composable with a normal call
        quickFixes.register(
            ComposeErrors.SVC_INVOCATION,
            ConvertCallToKtxQuickFix
        )

        // "Add Import" quick fixes for unresolved attributes that have valid extension attributes
        quickFixes.register(
            ComposeErrors.MISMATCHED_ATTRIBUTE_TYPE,
            ImportAttributeFix
        )
        quickFixes.register(Errors.UNRESOLVED_REFERENCE,
            ImportAttributeFix
        )
        quickFixes.register(
            ComposeErrors.UNRESOLVED_ATTRIBUTE_KEY,
            ImportAttributeFix
        )
        quickFixes.register(
            ComposeErrors.UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE,
            ImportAttributeFix
        )

        // "Add Import" for unresolved tags on KTX elements. The normal "Unresolved Reference" quick fix
        // doesn't work in this case because if the KTX element has an open and closing tag, only one of
        // them gets changes, and then the tag becomes invalid, and then only the one tag changes. We
        // wrote our own custom add import quick fix that handles it correctly, but it must use the
        // UNRESOLVED_TAG diagnostic in order to not compete with the built-in one. If JetBrains adds
        // a better prioritization system for quick fixes, we could use `UNRESOLVED_TAG` only if we
        // wanted.
        quickFixes.register(Errors.UNRESOLVED_REFERENCE,
            ImportComponentFix
        )
        quickFixes.register(
            ComposeErrors.UNRESOLVED_TAG,
            ImportComponentFix
        )

        // when a composer isn't in scope, this proovides a quickfix to import one
        quickFixes.register(
            ComposeErrors.NO_COMPOSER_FOUND,
            ImportComposerFix
        )
    }
}
