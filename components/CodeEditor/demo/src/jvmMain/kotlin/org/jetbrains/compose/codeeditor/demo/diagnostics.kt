package org.jetbrains.compose.codeeditor.demo

import org.jetbrains.compose.codeeditor.diagnostics.DiagnosticElement
import org.jetbrains.compose.codeeditor.diagnostics.Severity
import java.util.regex.Pattern

private val diagnosticsLinePatterns: List<Triple<Pattern, Severity, String>> = listOf(
    Triple(Pattern.compile("\\bfun (foo)\\(\\)"), Severity.INFO, "Function \"foo\" is never used"),
    Triple(Pattern.compile("\\bval (str)\\b"), Severity.INFO, "Variable \"str\" is never used"),
    Triple(Pattern.compile("\\b(vl)\\b"), Severity.ERROR, "Unresolved reference: vl"),
    Triple(Pattern.compile("\\b(list)\\b"), Severity.ERROR, "Unresolved reference: list"),
    Triple(Pattern.compile("\\bif \\((true)\\)"), Severity.INFO, "Condition is always \"true\""),
)

private val unusedLambdaPattern = Pattern.compile("\\{\\s*val str = \"[^\"]*\"\\s*}", Pattern.MULTILINE)

fun updateDiagnostics(code: String, list: MutableList<DiagnosticElement>) {
    list.clear()
    code.lineSequence().forEachIndexed { index, line ->
        diagnosticsLinePatterns.forEach { pattern ->
            pattern.first.matcher(line).results().forEach { result ->
                list.add(DiagnosticElement(
                    startLine = index + 1,
                    startCharacter = result.start(1),
                    endLine = index + 1,
                    endCharacter = result.end(1),
                    message = pattern.third,
                    severity = pattern.second
                ))
            }
        }
    }
    val matcher = unusedLambdaPattern.matcher(code)
    if (matcher.find()) {
        matcher.toMatchResult()?.let {
            val start = it.start()
            val end = it.end()
            val subSequenceBefore = code.subSequence(0, start)
            val startLine = subSequenceBefore.count { it == '\n' } + 1
            val endLine = startLine + code.subSequence(start, end).count { it == '\n' }
            var lastIndexOf = subSequenceBefore.lastIndexOf('\n', start)
            val startCharacter = if (lastIndexOf != -1) start - lastIndexOf - 1 else start
            lastIndexOf = code.subSequence(0, end).lastIndexOf('\n', end)
            val endCharacter = if (lastIndexOf != -1) end - lastIndexOf - 1 else end
            list.add(DiagnosticElement(
                startLine = startLine,
                startCharacter = startCharacter,
                endLine = endLine,
                endCharacter = endCharacter,
                message = "The lambda expression is unused. If you mean a block, you can use 'run { ... }'",
                severity = Severity.WARNING
            ))
        }
    }
}
