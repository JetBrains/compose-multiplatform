/* Copyright 2026 JetBrains s.r.o. Licensed under the Apache License, Version 2.0. */
import java.io.File
import java.nio.file.Files
import kotlin.test.Test

private data class RewriteCase(
    val name: String,
    val imports: String,
    val body: String,
    val expectedText: String,
    val rewritten: Boolean,
)

private val rewriteCases = listOf(
    RewriteCase("dynamic", "", "fun page(value: String) { Span { Text(value) } }", "LoweredDynamicTextTag", true),
    RewriteCase("getter", "", "fun page() { Div { Text(nextValue()) } }", "LoweredDynamicTextTag", true),
    RewriteCase("receiver", "", "fun page() { Div { Text(this.toString()) } }", "this.toString()", true),
    RewriteCase("const-shadow", "", "private const val label=\"literal\"\nfun page(label: String) { Span { Text(label) } }", "LoweredDynamicTextTag", true),
    RewriteCase("implicit-it-shadow", "", "private const val it=\"literal\"\nfun page() { listOf(\"runtime\").forEach { Span { Text(it) } } }", "LoweredDynamicTextTag", true),
    RewriteCase("foreign-repeat", "import other.repeat\n", "fun page() { Div { repeat(3) { Span { Text(\"literal\") } } } }", "Lowered", false),
    RewriteCase("subtree", "", "fun page() { Div { Span { Text(\"<&é😀\") }; P { Text(\"two\") } } }", "LoweredStaticHtmlFragment", true),
    RewriteCase("nested-containers", "", "fun page() { Div { Span { Text(nextValue()) }; Div { page() } } }", "LoweredHtmlContainer", true),
    RewriteCase("adjacent-text", "", "fun page() { Div { Span { Text(\"a\"); Text(\"b\") } } }", "LoweredStaticHtmlFragment", false),
    RewriteCase("dynamic-attrs", "", "fun page(id: String) { Span(attrs={ id(id) }) { Text(\"a\") } }", "Lowered", false),
    RewriteCase("event", "", "fun page() { Span(attrs={ onClick { println(\"event\") } }) { Text(\"a\") } }", "Lowered", false),
    RewriteCase("style", "", "fun page() { Span(attrs={ style { color(\"red\") } }) { Text(\"a\") } }", "Lowered", false),
    RewriteCase("alias", "import other.Other as Span\n", "fun page() { Span { Text(\"a\") } }", "Lowered", false),
    RewriteCase("star", "import other.*\n", "fun page() { Span { Text(\"a\") } }", "Lowered", false),
    RewriteCase("local-shadow", "", "fun page(Span: () -> Unit) { Span { Text(\"a\") } }", "Lowered", false),
    RewriteCase("qualified", "", "fun page() { other.Span { Text(\"a\") } }", "Lowered", false),
    RewriteCase("tag-return", "", "fun page() { Div { return@Div } }", "Lowered", false),
    RewriteCase("text-return", "", "fun page() { Span { Text(run { return@Span }) } }", "Lowered", false),
    RewriteCase("raw", "", "fun page() { Script { Text(\"x\") } }", "Lowered", false),
    RewriteCase("property", "", "fun page() { Span(attrs={ prop { it.textContent=\"x\" } }) { Text(\"a\") } }", "Lowered", false),
    RewriteCase("spread", "", "fun page() { Span(attrs={ classes(*arrayOf(\"x\")) }) { Text(\"a\") } }", "Lowered", false),
    RewriteCase("protocol", "", "fun page() { Span(attrs={ attr(\"data-compose-hydration-id\",\"x\") }) { Text(\"a\") } }", "Lowered", false),
)

private data class LoopCase(val name: String, val body: String, val folded: Boolean)

private val loopCases = listOf(
    LoopCase("literal-loop", "fun page() { Div { repeat(3) { Span { Text(\"literal\") } } } }", true),
    LoopCase("foreign-repeat-loop", "import other.repeat\nfun page() { Div { repeat(3) { Span { Text(\"literal\") } } } }", false),
    LoopCase("dynamic-loop", "fun page(n: Int) { Div { repeat(n) { Span { Text(\"literal\") } } } }", false),
    LoopCase("index-text", "fun page() { Div { repeat(3) { Span { Text(it.toString()) } } } }", false),
    LoopCase("shadow-repeat", "fun repeat(n: Int, content: () -> Unit) {}\nfun page() { Div { repeat(3) { Span { Text(\"literal\") } } } }", false),
    LoopCase("effect-loop", "fun page() { Div { repeat(3) { println(\"side effect\"); Span { Text(\"literal\") } } } }", false),
    LoopCase("adjacent-loop-text", "fun page() { Div { repeat(3) { Text(\"literal\") } } }", false),
    LoopCase("large-loop", "fun page() { Div { repeat(10_001) { Span { Text(\"literal\") } } } }", false),
)

private const val HEADER = "package fixtures\nimport org.jetbrains.compose.web.dom.*\n"

private fun generate(root: File, name: String, body: String, mode: String? = null): Pair<String, String> {
    val input = root.resolve("$name/input").apply { mkdirs() }
    val source = input.resolve("Fixture.kt")
    val original = HEADER + body + "\n"
    source.writeText(original)
    val output = root.resolve("$name/output")
    val args = listOf(input.absolutePath, output.absolutePath) + listOfNotNull(mode?.let { "--mode=$it" })
    main(args.toTypedArray())
    check(source.readText() == original) { "$name: authored source changed" }
    return output.resolve("Fixture.kt").readText() to output.resolve("lowering-report.tsv").readText()
}

class LowerStaticTest {
    @Test
    fun transformationsAndFallbacks() {
        val root = Files.createTempDirectory("html-lowering-checks-").toFile()
        try {
            for (case in rewriteCases) {
                val body = case.imports + case.body
                val (generated, _) = generate(root, case.name, body, "containers")
                check(generated.contains(case.expectedText) == case.rewritten) { "${case.name}: $generated" }
                if (case.expectedText == "Lowered" && !case.rewritten) {
                    check(generated == HEADER + body + "\n") { "${case.name}: fallback changed source" }
                }
            }
            for (case in loopCases) {
                val (_, report) = generate(root, case.name, case.body, "loops")
                // Check the whole loop; its inner span may be lowered separately.
                val folded = report.lineSequence().drop(1).any { "\tDiv\tstatic-subtree" in it }
                check(folded == case.folded) { "${case.name}: $report" }
            }
            val (defaultOutput, _) = generate(root, "default", "fun page() { Div { repeat(3) { Span { Text(\"literal\") } } } }")
            check("LoweredStaticHtmlFragment" in defaultOutput) { "Default mode did not fold the literal loop" }
            println("${rewriteCases.size + loopCases.size + 1} transformation and fallback checks passed.")
        } finally {
            root.deleteRecursively()
        }
    }
}
