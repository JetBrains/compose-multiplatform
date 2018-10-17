package org.jetbrains.kotlin.r4a.idea.quickfix

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.QuickFixContributor
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.r4a.analysis.R4AErrors

class R4aQuickFixRegistry : QuickFixContributor {
    override fun registerQuickFixes(quickFixes: QuickFixes) {

        // "Add Import" quick fixes for unresolved attributes that have valid extension attributes
        quickFixes.register(R4AErrors.MISMATCHED_ATTRIBUTE_TYPE, ImportAttributeFix)
        quickFixes.register(Errors.UNRESOLVED_REFERENCE, ImportAttributeFix)
        quickFixes.register(R4AErrors.UNRESOLVED_ATTRIBUTE_KEY, ImportAttributeFix)

        // "Add Import" for unresolved tags on KTX elements. The normal "Unresolved Reference" quick fix
        // doesn't work in this case because if the KTX element has an open and closing tag, only one of
        // them gets changes, and then the tag becomes invalid, and then only the one tag changes. We
        // wrote our own custom add import quick fix that handles it correctly, but it must use the
        // UNRESOLVED_TAG diagnostic in order to not compete with the built-in one. If JetBrains adds
        // a better prioritization system for quick fixes, we could use `UNRESOLVED_TAG` only if we
        // wanted.
        quickFixes.register(Errors.UNRESOLVED_REFERENCE, ImportComponentFix)
        quickFixes.register(R4AErrors.UNRESOLVED_TAG, ImportComponentFix)
    }
}
