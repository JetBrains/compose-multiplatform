package org.jetbrains.compose.codeeditor.diagnostics

/**
 * Contains information about the diagnostic message and its location in the code.
 *
 * Line numbering begins with 1.
 * Character numbering begins with 0.
 */
data class DiagnosticElement(
    val startLine: Int,
    val startCharacter: Int,
    val endLine: Int,
    val endCharacter: Int,
    val message: String,
    val severity: Severity
)
