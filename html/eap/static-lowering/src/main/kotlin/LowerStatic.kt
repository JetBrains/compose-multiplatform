/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(
    org.jetbrains.kotlin.K1Deprecation::class,
    org.jetbrains.kotlin.CoreEnvironmentDeprecation::class,
    org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class,
    org.jetbrains.kotlin.config.CompilerConfiguration.Internals::class,
)

import java.io.File
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*

/**
 * Precompute literal HTML before the Compose compiler runs.
 * Dynamic expressions stay in their lambdas. Ambiguous names keep their original code.
 * Checks syntax and imports, not resolved symbols.
 */
private const val DOM = "org.jetbrains.compose.web.dom."
// Only these tags have lowering helpers. Other tags keep their original code.
private val tags =
    mapOf(
        "Div" to "HTMLDivElement",
        "Span" to "HTMLSpanElement",
        "P" to "HTMLParagraphElement",
        "H1" to "HTMLHeadingElement",
        "H2" to "HTMLHeadingElement",
        "H3" to "HTMLHeadingElement",
        "H4" to "HTMLHeadingElement",
        "H5" to "HTMLHeadingElement",
        "H6" to "HTMLHeadingElement",
        "Td" to "HTMLTableCellElement",
        "Th" to "HTMLTableCellElement",
        "Tr" to "HTMLTableRowElement",
    )
private val intrinsics = tags.keys + setOf("Text", "classes", "attr", "id", "repeat")

private fun literal(value: KtExpression?): Boolean =
    value is KtStringTemplateExpression &&
        !value.text.startsWith("\"\"\"") &&
        value.entries.all { it is KtLiteralStringTemplateEntry } &&
        !value.text.contains('\u0000')

private fun plain(value: KtExpression): String = value.text.removeSurrounding("\"")

private fun quoted(value: String) =
    "\"" +
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") +
        "\""

// Split large strings to fit the JVM constant pool.
private fun encodedExpression(value: String): String =
    if (value.length <= 8000) quoted(value)
    else
        value.chunked(8000).joinToString(
            prefix = "listOf(",
            postfix = ").joinToString(" + quoted("") + ")",
            separator = ",",
        ) {
            quoted(it)
        }

private fun escaped(value: String, attribute: Boolean): String = buildString {
    for (c in value) append(
        when (c) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '\r' -> "&#13;"
            '"' -> if (attribute) "&quot;" else c.toString()
            '\n' -> if (attribute) "&#10;" else c.toString()
            '\t' -> if (attribute) "&#9;" else c.toString()
            else -> c.toString()
        }
    )
}

// Accept only literal attributes.
private fun attrsPrefix(tag: String, attrs: KtLambdaExpression?): String? {
    if (attrs?.valueParameters?.isNotEmpty() == true) return null
    val classes = mutableListOf<String>()
    val values = linkedMapOf<String, String>()
    for (statement in attrs?.bodyExpression?.statements.orEmpty()) {
        val call = statement as? KtCallExpression ?: return null
        if (
            call.lambdaArguments.isNotEmpty() ||
                call.valueArguments.any { it.isNamed() || it.getSpreadElement() != null }
        )
            return null
        val args = call.valueArguments.map { it.getArgumentExpression() }
        if (!args.all(::literal)) return null
        val strings = args.map { plain(it!!) }
        when (call.calleeExpression?.text) {
            "classes" -> {
                if (strings.any { it.isEmpty() || it.any { c -> c in " \t\n\r\u000c" } })
                    return null
                classes.addAll(strings)
            }
            "attr",
            "id" -> {
                val pair =
                    if (call.calleeExpression?.text == "id" && strings.size == 1) "id" to strings[0]
                    else if (call.calleeExpression?.text == "attr" && strings.size == 2)
                        strings[0] to strings[1]
                    else return null
                if (
                    !pair.first.matches(
                        Regex("(?:data-[a-z0-9-]+|aria-[a-z0-9-]+|title|id|role)")
                    ) || pair.first.startsWith("data-compose") || pair.first in values
                )
                    return null
                values[pair.first] = pair.second
            }
            else -> return null
        }
    }
    if (classes.distinct().size != classes.size) return null
    return buildString {
        append('<').append(tag)
        for ((name, value) in values) append(' ')
            .append(name)
            .append("=\"")
            .append(escaped(value, true))
            .append('"')
        if (classes.isNotEmpty())
            append(" class=\"").append(escaped(classes.joinToString(" "), true)).append('"')
        append('>')
    }
}

private data class Replacement(val start: Int, val end: Int, val text: String)

fun main(args: Array<String>) {
    require(
        args.size >= 2 &&
            args.drop(2).all { it.startsWith("--trusted-star-import=") || it.startsWith("--mode=") }
    )
    val mode = args.firstOrNull { it.startsWith("--mode=") }?.substringAfter('=') ?: "loops"
    require(mode in setOf("round4", "dynamic", "blocks", "containers", "loops"))
    val trustedStarImports =
        args
            .drop(2)
            .filter { it.startsWith("--trusted-star-import=") }
            .map { it.substringAfter('=') }
            .toSet() +
            setOf("org.jetbrains.compose.web.dom", "org.jetbrains.compose.web.attributes")
    val disposable = Disposer.newDisposable()
    try {
        val environment =
            KotlinCoreEnvironment.createForProduction(
                disposable,
                CompilerConfiguration().apply {
                    extensionsStorage = CompilerPluginRegistrar.ExtensionStorage()
                },
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )
        val factory = KtPsiFactory(environment.project)
        val input = File(args[0])
        val output = File(args[1])
        val files =
            input
                .walkTopDown()
                .filter { it.extension == "kt" }
                .sortedBy { it.path }
                .map { it to factory.createFile(it.name, it.readText()) }
                .toList()
        // A custom DSL name disables rewriting.
        val shadowed =
            files
                .flatMap { (_, f) ->
                    PsiTreeUtil.collectElementsOfType(f, KtNamedDeclaration::class.java)
                        .mapNotNull { it.name }
                }
                .toSet()
                .intersect(intrinsics)
        val report = mutableListOf<String>()
        for ((path, file) in files) {
            val source = file.text
            val replacements = mutableListOf<Replacement>()
            val declarations = mutableListOf<String>()
            val shared = mutableMapOf<String, String>()
            val texts = mutableMapOf<String, String>()
            val imports =
                file.importDirectives
                    .filter { it.aliasName == null }
                    .mapNotNull { it.importedFqName?.asString() + if (it.isAllUnder) ".*" else "" }
                    .toSet()
            val aliases =
                file.importDirectives
                    .filter { it.aliasName != null }
                    .flatMap {
                        listOfNotNull(it.aliasName, it.importedFqName?.shortName()?.asString())
                    }
                    .toSet()
            val declarationsInFile =
                PsiTreeUtil.collectElementsOfType(file, KtNamedDeclaration::class.java)
            val constants =
                PsiTreeUtil.collectElementsOfType(file, KtProperty::class.java)
                    .filter { property ->
                        // A lambda's implicit it can hide this constant.
                        property.name != "it" &&
                            declarationsInFile.count { it.name == property.name } == 1 &&
                            property.hasModifier(
                                org.jetbrains.kotlin.lexer.KtTokens.CONST_KEYWORD
                            ) &&
                            literal(property.initializer)
                    }
                    .associate { it.name!! to it.initializer!! }
            // Trust star imports only when the caller opts in.
            val conflictingImports =
                file.importDirectives.any {
                    val fq = it.importedFqName?.asString().orEmpty()
                    val short = it.importedFqName?.shortName()?.asString()
                    !it.isAllUnder &&
                        ((short in (tags.keys + "Text") && fq != DOM + short) ||
                            (short == "repeat" && fq != "kotlin.repeat"))
                }
            val unknownStarImport =
                file.importDirectives.any {
                    it.isAllUnder && it.importedFqName?.asString() !in trustedStarImports
                }
            val generatedNames = declarationsInFile.mapNotNull { it.name }.toMutableSet()
            fun uniqueName(prefix: String): String {
                var suffix = 0
                while (!generatedNames.add(prefix + suffix)) suffix++
                return prefix + suffix
            }
            if (
                !unknownStarImport &&
                    !conflictingImports &&
                    intrinsics.none { it in shadowed || it in aliases } &&
                    (DOM + "*" in imports || DOM + "Text" in imports)
            ) {
                // Encode the whole subtree only if every statement is constant.
                fun staticTree(call: KtCallExpression): String? {
                    val name =
                        (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
                            ?: return null
                    if (name !in tags || (DOM + "*" !in imports && DOM + name !in imports))
                        return null
                    if (
                        call.parent is KtDotQualifiedExpression ||
                            call.parent is KtSafeQualifiedExpression
                    )
                        return null
                    val args = call.valueArguments.filter { it !is KtLambdaArgument }
                    if (args.size > 1) return null
                    val attrs =
                        args.singleOrNull()?.let { arg ->
                            if (
                                arg.getArgumentName()?.asName?.asString()?.let { it != "attrs" } ==
                                    true
                            )
                                null
                            else arg.getArgumentExpression() as? KtLambdaExpression
                        }
                    if (args.isNotEmpty() && attrs == null) return null
                    val prefix = attrsPrefix(name.lowercase(), attrs) ?: return null
                    val body =
                        call.lambdaArguments.singleOrNull()?.getLambdaExpression() ?: return null
                    if (body.valueParameters.isNotEmpty()) return null
                    var previousText = false
                    val html = StringBuilder(prefix)
                    for (statement in body.bodyExpression?.statements.orEmpty()) {
                        val child = statement as? KtCallExpression ?: return null
                        if (child.calleeExpression?.text == "Text") {
                            if (child.lambdaArguments.isNotEmpty()) return null
                            val arg = child.valueArguments.singleOrNull() ?: return null
                            if (
                                arg.getArgumentName()?.asName?.asString()?.let { it != "value" } ==
                                    true
                            )
                                return null
                            val value = arg.getArgumentExpression()
                            val constant =
                                when {
                                    literal(value) -> value
                                    value is KtNameReferenceExpression ->
                                        constants[value.getReferencedName()]
                                    else -> null
                                } ?: return null
                            val raw = plain(constant)
                            if (raw.isNotEmpty()) {
                                // Mark adjacent text so the whole subtree falls back.
                                if (previousText) html.append("<!--compose-text-boundary-->")
                                html.append(escaped(raw, false))
                                previousText = true
                            }
                        } else if (mode == "loops" && child.calleeExpression?.text == "repeat") {
                            val times =
                                child.valueArguments
                                    .filter { it !is KtLambdaArgument }
                                    .singleOrNull()
                                    ?.getArgumentExpression()
                                    ?.text
                                    ?.replace("_", "")
                                    ?.toIntOrNull() ?: return null
                            // Limit expansion; index-dependent values and effects fall back.
                            if (times !in 0..10_000) return null
                            val loop =
                                child.lambdaArguments.singleOrNull()?.getLambdaExpression()
                                    ?: return null
                            if (loop.valueParameters.isNotEmpty()) return null
                            // Wrap the loop body in Div to reuse subtree checks.
                            val wrapped =
                                factory.createExpression("Div " + loop.text) as KtCallExpression
                            val inside =
                                (staticTree(wrapped) ?: return null)
                                    .removePrefix("<div>")
                                    .removeSuffix("</div>")
                            if (
                                inside.isNotEmpty() &&
                                    (!inside.startsWith("<") || !inside.endsWith(">"))
                            )
                                return null
                            if (inside.length.toLong() * times + html.length > 2_000_000)
                                return null
                            html.append(inside.repeat(times))
                            previousText = false
                        } else {
                            html.append(staticTree(child) ?: return null)
                            previousText = false
                        }
                    }
                    return html.append("</").append(name.lowercase()).append('>').toString()
                }
                // Replace outer trees first, then skip their children.
                val blockRanges = mutableListOf<IntRange>()
                for (call in
                    PsiTreeUtil.collectElementsOfType(file, KtCallExpression::class.java).sortedBy {
                        it.textRange.startOffset
                    }) {
                    if (blockRanges.any { call.textRange.startOffset in it }) continue
                    if (mode == "blocks" || mode == "containers" || mode == "loops") {
                        val name = call.calleeExpression?.text
                        // Only the fragment root needs a parent parser check.
                        if (name in setOf("Div", "Span", "P", "H1", "H2", "H3", "H4", "H5", "H6")) {
                            val html = staticTree(call)
                            if (
                                html != null &&
                                    PsiTreeUtil.collectElementsOfType(
                                            call,
                                            KtCallExpression::class.java,
                                        )
                                        .count { it.calleeExpression?.text in tags } > 1 &&
                                    !html.contains("<!--compose-text-boundary-->")
                            ) {
                                val holder = uniqueName("__ssrStaticFragment")
                                declarations +=
                                    "private val $holder = ${DOM}PrecomputedHtmlFragment(${quoted(name!!.lowercase())}, ${encodedExpression(html)}) { ${call.text} }"
                                replacements +=
                                    Replacement(
                                        call.textRange.startOffset,
                                        call.textRange.endOffset,
                                        "${DOM}LoweredStaticHtmlFragment($holder)",
                                    )
                                blockRanges +=
                                    call.textRange.startOffset until call.textRange.endOffset
                                report +=
                                    "${path.relativeTo(input)}\t${source.take(call.textRange.startOffset).count { it=='\n' }+1}\t$name\tstatic-subtree"
                                continue
                            }
                        }
                    }
                    val name =
                        (call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
                            ?: continue
                    val type = tags[name] ?: continue
                    if (DOM + "*" !in imports && DOM + name !in imports) continue
                    if (
                        call.parent is KtDotQualifiedExpression ||
                            call.parent is KtSafeQualifiedExpression
                    )
                        continue
                    val args = call.valueArguments.filter { it !is KtLambdaArgument }
                    if (args.size > 1) continue
                    val attrs =
                        args.singleOrNull()?.let {
                            if (
                                it.getArgumentName()?.asName?.asString()?.let { n ->
                                    n != "attrs"
                                } == true
                            )
                                null
                            else it.getArgumentExpression() as? KtLambdaExpression
                        }
                    if (args.isNotEmpty() && attrs == null) continue
                    val tag = name.lowercase()
                    val prefix = attrsPrefix(tag, attrs) ?: continue
                    val body = call.lambdaArguments.singleOrNull()?.getLambdaExpression()
                    val textCall =
                        body
                            ?.takeIf { it.valueParameters.isEmpty() }
                            ?.bodyExpression
                            ?.statements
                            ?.singleOrNull() as? KtCallExpression
                    val value =
                        textCall
                            ?.takeIf {
                                it.calleeExpression?.text == "Text" && it.lambdaArguments.isEmpty()
                            }
                            ?.valueArguments
                            ?.singleOrNull()
                            ?.getArgumentExpression()
                    // Move only constant text; evaluate calls and getters in place.
                    val constant =
                        when {
                            literal(value) -> value
                            value is KtNameReferenceExpression ->
                                constants[value.getReferencedName()]
                            else -> null
                        }
                    val key = tag + ":" + (attrs?.text ?: "{}")
                    if (
                        attrs == null &&
                            constant == null &&
                            (mode == "round4" ||
                                (value == null && mode != "containers" && mode != "loops"))
                    )
                        continue
                    val holder =
                        shared.getOrPut(key) {
                            val id = uniqueName("__ssrStaticAttributes")
                            declarations +=
                                "private val $id = ${DOM}precomputedHtmlAttributes<kotlinx.browser.dom.$type>(${quoted(tag)}, ${quoted(prefix)}) ${attrs?.text ?: "{}"}"
                            id
                        }
                    if (constant != null) {
                        val raw = plain(constant)
                        val text =
                            texts.getOrPut(raw) {
                                val id = uniqueName("__ssrStaticText")
                                declarations +=
                                    "private val $id = ${DOM}precomputedHtmlText(${quoted(raw)}, ${quoted(escaped(raw,false))})"
                                id
                            }
                        replacements +=
                            Replacement(
                                call.textRange.startOffset,
                                call.textRange.endOffset,
                                "${DOM}LoweredTextTag($holder, $text)",
                            )
                        report +=
                            "${path.relativeTo(input)}\t${source.take(call.textRange.startOffset).count { it=='\n' }+1}\t$name\ttext"
                    } else if (
                        value != null &&
                            mode != "round4" &&
                            !Regex("\\b(return|break|continue)\\b|this@")
                                .containsMatchIn(value.text)
                    ) {
                        replacements +=
                            Replacement(
                                call.textRange.startOffset,
                                call.textRange.endOffset,
                                "${DOM}LoweredDynamicTextTag($holder) { ${value.text} }",
                            )
                        report +=
                            "${path.relativeTo(input)}\t${source.take(call.textRange.startOffset).count { it=='\n' }+1}\t$name\tdynamic-text"
                    } else if (
                        (mode == "containers" || mode == "loops") &&
                            body != null &&
                            !Regex("\\b(return|break|continue)\\b|this@").containsMatchIn(body.text)
                    ) {
                        // Keep the content lambda and its element scope.
                        replacements +=
                            Replacement(
                                call.textRange.startOffset,
                                body.textRange.startOffset,
                                "${DOM}LoweredHtmlContainer($holder) ",
                            )
                        report +=
                            "${path.relativeTo(input)}\t${source.take(call.textRange.startOffset).count { it=='\n' }+1}\t$name\tcontainer"
                    } else if (attrs != null) {
                        replacements +=
                            Replacement(
                                attrs.textRange.startOffset,
                                attrs.textRange.endOffset,
                                "$holder.callback",
                            )
                        report +=
                            "${path.relativeTo(input)}\t${source.take(call.textRange.startOffset).count { it=='\n' }+1}\t$name\tattributes"
                    }
                }
            }
            // Edit from the end so earlier offsets stay valid.
            val generated = StringBuilder(source)
            for ((start, end, value) in replacements.sortedByDescending { it.start }) generated
                .replace(start, end, value)
            val usedDeclarations = declarations.filter { declaration ->
                val name = declaration.substringAfter("private val ").substringBefore(" = ")
                val reference = Regex("\\b" + Regex.escape(name) + "\\b")
                replacements.any { reference.containsMatchIn(it.text) }
            }
            if (usedDeclarations.isNotEmpty()) {
                generated
                    .append("\n// Generated static HTML fragments.\n")
                    .append(usedDeclarations.joinToString("\n"))
                    .append('\n')
            }
            val target = output.resolve(path.relativeTo(input))
            target.parentFile.mkdirs()
            target.writeText(generated.toString())
        }
        output
            .resolve("lowering-report.tsv")
            .writeText("file\tline\ttag\tkind\n" + report.joinToString("\n") + "\n")
        println("Lowered ${report.size} sites; authored sources unchanged.")
    } finally {
        Disposer.dispose(disposable)
    }
}
