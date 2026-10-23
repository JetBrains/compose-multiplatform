/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.html.compiler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WrapperLoweringTest {
    @Test
    fun wrapperArgumentsAndSpecializedScopesPreserveRuntimeBehavior() {
        comparison.assertEquivalent(behaviorCases.keys)
    }

    @Test
    fun wrappersSelectDirectWritersAndRejectUnsupportedOverloads() {
        val calls = comparison.writerCalls
        for (name in listOf(
            "WrapperParameters", "FormActions", "TypedValues", "ValuePrecedence",
            "CheckedPrecedence", "TextAreaValues", "ListenerEffects", "DirectElements", "ArgumentOrder",
            "BoundListener", "CustomInputType", "DynamicInput", "SvgStyles", "TextAreaStyles",
        )) assertTrue((calls.getValue(name)["finishMixedElement"] ?: 0) > 0, "Expected mixed lowering: $name")
        assertEquals(2, calls.getValue("SvgOverride")["finishMixedElement"])
        for (name in listOf("UnmarkedInputOverloads", "SvgDuplicate", "SvgDuplicateBranches")) {
            assertEquals(emptyMap(), calls.getValue(name), name)
        }
        assertTrue(methodStrings(comparison.bytecode, "LiteralListener").any { "<button class=\"buy\">Buy</button>" in it })
    }

    @Test
    fun metadataNamesAndConversionsUseDirectWriters() {
        comparison.assertEquivalent(metadataCases.keys)
        for (name in metadataCases.keys) {
            assertTrue((comparison.writerCalls.getValue(name)["finishMixedElement"] ?: 0) > 0,
                "Expected annotated attributes to lower directly: $name")
        }
    }

    @Test
    fun omittedDefaultsMatchExplicitArguments() {
        comparison.assertEquivalent(defaultCases.keys)
        for (name in defaults.keys) {
            assertEquals(comparison.ordinary.getValue("Omitted$name"), comparison.ordinary.getValue("Explicit$name"),
                "Marker default drift: $name")
        }
    }

    companion object {
        // One example per metadata behavior; form control and SVG interactions have their own tests.
        private val metadataCases = mapOf(
            "InferredNamesAndPrimitiveValues" to """
                Div(attrs = { title(Mark(name)); tabIndex(3); contentEditable(true) })
            """.trimIndent(),
            "ExplicitHtmlName" to "Form(attrs = { acceptCharset(Mark(name)) })",
            "EnumPropertyConversion" to "Div(attrs = { dir(DirType.Rtl); inputMode(InputMode.Text); title(Mark(name)) })",
            "EnumToStringConversion" to "Input(InputType.Text) { autoComplete(AutoComplete.off); title(Mark(name)) }",
            "BooleanOnOffConversion" to "Form(attrs = { autoComplete(false); title(Mark(name)) })",
            "ConstantAndEmptyValues" to "Button(attrs = { disabled(); hidden(); title(Mark(name)) })",
            "InheritedMember" to "Input(InputType.Text) { lang(Mark(name)) }",
            "SvgAttributeNames" to "Svg { G(attrs = { viewBox(Mark(name)); fillRule(\"evenodd\") }) }",
            "SvgNumberFormatting" to "Svg { G(attrs = { width(3); fillOpacity(1.25); id(Mark(name)) }) }",
            "SvgCssValueConversion" to "Svg { G(attrs = { width(3.px); fillOpacity(25.percent); id(Mark(name)) }) }",
        )

        // Representative enum, string, boolean, nullable and numeric defaults.
        // Other wrappers exercise these same forms in the behavior cases.
        private val defaults = mapOf(
            "HelperTarget" to ("A(attrs = { target() })" to "A(attrs = { target(ATarget.Self) })"),
            "HelperDownload" to ("A(attrs = { download() })" to "A(attrs = { download(\"\") })"),
            "HelperAutoComplete" to ("Form(attrs = { autoComplete() })" to "Form(attrs = { autoComplete(true) })"),
            "Range" to ("RangeInput()" to "RangeInput(null, null, null, 1)"),
            "Img" to ("Img(\"src\")" to "Img(\"src\", \"\")"),
            "SvgText" to ("Svg { SvgText(\"text\") }" to "Svg { SvgText(\"text\", 0, 0) }"),
        )
        private val defaultCases = buildMap {
            for ((name, pair) in defaults) {
                put("Omitted$name", pair.first)
                put("Explicit$name", pair.second)
            }
        }

        private val behaviorCases = linkedMapOf(
            "LiteralListener" to "Button(attrs = { classes(\"buy\"); onClick { Mark(\"unused\") } }) { Text(\"Buy\") }",
            "UncoveredParameters" to "Uncovered(); Uncovered(mode = null); WrongReceiver(); WrongReceiver(href = null)",
            "CustomInputType" to """
                Input(InputType.InputTypeWithStringValue("hidden")) {
                    value(Mark("controlled"))
                    attr("data-mid", "m")
                    defaultValue("default")
                }
            """.trimIndent(),
            "BoundListener" to "Button(attrs = { onClick(clickTarget()::click); attr(\"data-x\", Mark(name)) }) { Text(name) }",
            "WrapperParameters" to """
                A(href = Mark(name), attrs = {
                    attr("data-first", Mark("a"))
                    attr("href", "override")
                }) {
                    Text(name)
                }
                Img(src = Mark(name))
                Label(forId = Mark(name)) {
                    Text(name)
                }
                OptGroup(label = Mark(name)) {
                    Option(Mark(name), attrs = {
                        selected()
                    }) {
                        Text(name)
                    }
                }
            """.trimIndent(),
            "FormActions" to """
                for (action in listOf(null, "", name)) Form(action = action, attrs = {
                    attr("data-x", Mark(name))
                    attr("action", Mark("override"))
                }) {
                    Text(name)
                }
                Form()
                Form(action = "")
            """.trimIndent(),
            "TypedValues" to """
                RadioInput(true) { attr("data-x", Mark(name)) }
                NumberInput(1.25, 0, 10) { value(3); defaultValue(7) }
                RangeInput(2, 0, 10, 0.5) { step(2); min("-1") }
            """.trimIndent(),
            "ValuePrecedence" to """
                Input(InputType.Text) {
                    value(Mark("first"))
                    attr("data-mid", "m")
                    defaultValue("default")
                    attr("data-last", "z")
                    value(Mark("last"))
                }
                Input(InputType.Text) {
                    attr("value", "default")
                    attr("data-last", "z")
                    value("controlled")
                }
            """.trimIndent(),
            "CheckedPrecedence" to """
                CheckboxInput {
                    defaultChecked()
                    attr("data-x", "x")
                }
                Input(InputType.Checkbox) {
                    checked(true)
                    attr("data-a", "a")
                    defaultChecked()
                    checked(false)
                }
                Input(InputType.Checkbox) {
                    checked(false)
                    attr("checked", "yes")
                    checked(true)
                }
            """.trimIndent(),
            "DynamicInput" to """
                for (type in listOf(InputType.Text, InputType.Hidden, InputType.Submit, InputType.Checkbox, InputType.Radio)) {
                    Input(type) {
                        value(Mark("controlled"))
                        attr("data-mid", "m")
                        defaultValue(Mark("default"))
                        onInput {
                        }
                    }
                    Input(type)
                }
                Input(InputType.Text)
            """.trimIndent(),
            "TextAreaValues" to """
                for (value in listOf(null, "", "\n<&", name)) {
                    TextArea(value = value, attrs = {
                        defaultValue(Mark("default"))
                        value(Mark("inner"))
                        defaultValue("last default")
                        rows(3)
                        onInput {
                        }
                    })
                    TextArea(value = value)
                }
                TextArea {
                    defaultValue("\n<&")
                    cols(2)
                }
                TextArea {
                    value("")
                    defaultValue("ignored")
                }
            """.trimIndent(),
            "ListenerEffects" to """
                TextInput {
                    onInput(listener = kotlin.run { Mark("listener"); { _ -> Unit } })
                    ref { Mark("ref should not run"); onDispose {} }
                    attr("data-x", Mark(name))
                }
                Button(attrs = {
                    onClick(listener = kotlin.run { Mark("click listener"); { _ -> Unit } })
                }) { Text(name) }
            """.trimIndent(),
            "DirectElements" to """
                TagElement<Element>(tagName = "time", applyAttrs = {
                    attr("datetime", Mark(name))
                }, content = {
                    Text(name)
                })
                TagElementNS<Element>("circle", "http://www.w3.org/2000/svg", {
                    attr("cx", Mark(name))
                }, null)
                TagElement<Element>(ElementBuilder.createBuilder("article"), null) {
                    Text(name)
                }
            """.trimIndent(),
            "ArgumentOrder" to """
                Img(alt = Mark("alt"), src = Mark("src"), attrs = {
                    attr("data-x", Mark(name))
                })
                Select(multiple = run {
                    Mark("multiple")
                    true
                }, attrs = {
                    attr("data-x", Mark("attrs"))
                })
                TextArea(attrs = {
                    defaultValue(Mark("attrs"))
                }, value = Mark("value"))
            """.trimIndent(),
            "UnsupportedWrappers" to """
                val attrs: org.jetbrains.compose.web.attributes.builders.InputAttrsScope<String>.() -> Unit = {
                    value(Mark(name))
                }
                TextInput(attrs = attrs)
                TextArea(attrs = {
                    customTextAreaHelper()
                })
                Img(Mark("src"), attrs = {
                    customImageHelper()
                })
            """.trimIndent(),
            "SvgNullableViewBox" to """
                Svg(viewBox = if (name.isEmpty()) null else Mark("0 0 50 50"), attrs = {
                    attr("preserveAspectRatio", "xMidYMid")
                    attr("hidden", "")
                }) {
                    Circle(1, 2, 3)
                    Probe()
                }
                Svg(viewBox = null) {
                    Circle(4, 5, 6)
                    Probe()
                }
                Svg {
                    Probe()
                }
            """.trimIndent(),
            "SvgOverride" to """
                Svg {
                    Circle(cx = 1, cy = 2, r = 3, attrs = {
                        attr("data-first", Mark(name))
                        attr("cx", "2")
                        attr("data-last", "last")
                    })
                    Circle(cx = MarkNumber(1.25), cy = MarkNumber(2), r = MarkNumber(3), attrs = {
                        attr("hidden", Mark(""))
                        attr("cx", Mark("2"))
                    }) {
                        Probe()
                    }
                }
            """.trimIndent(),
            "SvgDuplicate" to "Svg(viewBox = \"0 0 50 50\", attrs = { attr(\"viewbox\", Mark(name)) }) { Probe() }",
            "SvgDuplicateBranches" to """
                Svg(attrs = {
                    if (name.isNotEmpty()) attr("viewBox", Mark("first"))
                    attr("viewbox", Mark(name))
                }) {
                    Probe()
                }
            """.trimIndent(),
            "SvgCallbackFallback" to """
                Svg {
                    val callback: AttrBuilderContext<kotlinx.browser.dom.svg.SVGCircleElement> = {
                        attr("cx", Mark(name))
                    }
                    Circle(1, 2, 3, attrs = callback)
                    SvgElement<kotlinx.browser.dom.svg.SVGElement>("foreignObject") {
                        Div {
                            Text(name)
                            Probe()
                        }
                    }
                    Probe()
                }
            """.trimIndent(),
            "SvgTextAndCamelCase" to """
                Svg {
                    Path(Mark("M 0 0 L 5 5"))
                    SvgText(Mark(name), MarkNumber(1.25), MarkNumber(2))
                    Title("<title>&")
                    AnimateMotion()
                    TextPath("#path", name)
                    Probe()
                }
            """.trimIndent(),
            "SvgArgumentOrder" to """
                Svg {
                    Circle(r = MarkNumber(3), cx = MarkNumber(1), cy = MarkNumber(2), attrs = {
                        attr("data-value", Mark(name))
                    })
                    SvgText(y = MarkNumber(4), text = Mark(name), x = MarkNumber(5))
                    Scope(this).Circle(1, 2, 3)
                    Probe()
                }
            """.trimIndent(),
            "SvgStyles" to """
                Svg(attrs = {
                    style {
                        property("color", Mark(name))
                    }
                }) {
                    Circle(1, 2, 3, attrs = {
                        style {
                            property("fill", Mark("red"))
                        }
                    })
                    Probe()
                }
            """.trimIndent(),
            "TextAreaStyles" to """
                TextArea(value = Mark("\n<&"), attrs = {
                    defaultValue(Mark("default"))
                    style {
                        property("color", Mark(name))
                    }
                    if (name.isNotEmpty()) style {
                        property("display", Mark("none"))
                    }
                })
            """.trimIndent(),
            "MixedLet" to "Div(attrs = { name.let { value -> id(Mark(value)); attr(\"title\", Mark(value)) } }) { Text(name) }",
            "StyleException" to """
                Div(attrs = {
                    attr("title", Mark("before"))
                    style {
                        property("color", Mark(name))
                        error("style failure")
                    }
                    attr("data-after", Mark("after"))
                })
            """.trimIndent(),
            "LocalValueReturn" to """
                Div(attrs = {
                    attr("title", run {
                        fun title(): String {
                            return Mark(name)
                        }
                        title()
                    })
                }) {
                    Probe()
                }
            """.trimIndent(),
            "UnmarkedInputOverloads" to """
                org.jetbrains.compose.web.dom.Input(type = Mark(name), attrs = { Text("custom input") })
                org.jetbrains.compose.web.dom.Input(type = InputType.Text, attrs = null as String?)
            """.trimIndent(),
        )
        private val comparison by lazy {
            LoweringComparison(metadataCases + defaultCases + behaviorCases,
                matrixCases = setOf("SvgDuplicate", "SvgDuplicateBranches", "StyleException", "TextAreaValues", "SvgCallbackFallback"),
                declarations = """
                    @Composable @LowerableHtmlTag("div")
                    fun Uncovered(mode: String? = null, attrs: AttrBuilderContext<kotlinx.browser.dom.HTMLDivElement>? = null) {
                        Div(attrs) { Text("kept") }
                    }
                    @Composable @LowerableHtmlTag("input")
                    fun WrongReceiver(href: String? = null, attrs: AttrBuilderContext<kotlinx.browser.dom.HTMLInputElement>? = null) {
                        Input(InputType.Text) { attrs?.invoke(this) }
                    }
                    class ClickTarget { fun click(event: androidx.compose.web.events.SyntheticMouseEvent) {} }
                    fun clickTarget(): ClickTarget { Mark("bound receiver"); return ClickTarget() }
                    fun org.jetbrains.compose.web.attributes.builders.TextAreaAttrsScope.customTextAreaHelper() {
                        value(Mark("custom"))
                    }
                    fun org.jetbrains.compose.web.attributes.AttrsScope<kotlinx.browser.dom.HTMLImageElement>.customImageHelper() {
                        attr("src", Mark("custom"))
                    }
                    fun MarkNumber(value: Number): Number { Mark("number=${'$'}value"); return value }
                    fun Scope(scope: ElementScope<kotlinx.browser.dom.svg.SVGElement>): ElementScope<kotlinx.browser.dom.svg.SVGElement> {
                        Mark("scope")
                        return scope
                    }
                """.trimIndent(), extraSources = mapOf("ExtraInput.kt" to """
                    package org.jetbrains.compose.web.dom
                    import androidx.compose.runtime.Composable
                    @Composable fun Input(type: String, attrs: @Composable () -> Unit) { Text(type); attrs() }
                    @Composable fun Input(type: org.jetbrains.compose.web.attributes.InputType<*>, attrs: String?) { Text("custom typed input") }
                """.trimIndent()))
        }
    }
}
