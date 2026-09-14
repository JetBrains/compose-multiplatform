package org.jetbrains.compose.web

import kotlinx.browser.dom.HTMLDivElement
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Body
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Head
import org.jetbrains.compose.web.dom.Html
import org.jetbrains.compose.web.dom.Script
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Title
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HydratedDocumentTest {
    // Encodes and decodes hostile strings to verify that state transport is lossless.
    @Test
    fun transportEscapingRoundTripsAdversarialText() {
        val values = listOf(
            "",
            "plain text",
            "<",
            "</script>",
            "&lt;",
            "&amp;lt;",
            "&&<<",
            "\r",
            "\u0000",
            "\r\n",
            "<value>&lt;</script>\r\u0000</value>",
        )

        values.forEach { value ->
            assertEquals(
                value,
                value.escapeForHydrationStateElement().unescapeFromHydrationStateElement(),
                value,
            )
        }
    }

    // Renders one document and checks the exact doctype, root, state, and application-owned script.
    @Test
    fun rendersACompleteDocumentAndItsProtocol() {
        val initialState = TestState(label = "JVM state", count = 42)

        val rendered = renderHydratedDocument {
            Html {
                Head { Title { Text("Hydrated page") } }
                Body {
                    HydrationRoot(
                        initialState = initialState,
                        serializeState = TestState::toJson,
                        rootAttrs = { attr("class", "application") },
                    ) { state ->
                        Text("${state.label}: ${state.count}")
                    }
                    Script(attrs = {
                        attr("nonce", "request-nonce")
                        attr("integrity", "sha384-test")
                        attr("crossorigin", "anonymous")
                        type(ScriptType.Module)
                        src("/application.js")
                    })
                }
            }
        }

        assertEquals(
            "<!doctype html><html><head><title>Hydrated page</title></head><body>" +
                "<div class=\"application\" data-compose-hydration-root=\"\">" +
                "JVM state: 42</div>" +
                "<script data-compose-hydration-state=\"escaped-text-v1\" type=\"text/plain\">" +
                "{\"label\":\"JVM state\",\"count\":42}</script>" +
                "<script nonce=\"request-nonce\" integrity=\"sha384-test\" " +
                "crossorigin=\"anonymous\" type=\"module\" " +
                "src=\"/application.js\"></script></body></html>",
            rendered,
        )
    }

    // Records serializer and content calls to verify serialization happens once and first.
    @Test
    fun serializesExactlyOnceBeforeComposingContent() {
        val calls = mutableListOf<String>()

        renderHydratedDocument {
            Html {
                Body {
                    HydrationRoot(
                        initialState = "snapshot",
                        serializeState = {
                            calls += "serialize:$it"
                            it
                        },
                    ) { state ->
                        calls += "content:$state"
                        Text(state)
                    }
                }
            }
        }

        assertEquals(listOf("serialize:snapshot", "content:snapshot"), calls)
    }

    // Embeds script-breaking text and checks that no raw closing script sequence survives.
    @Test
    fun safelyEmbedsArbitrarySerializedText() {
        val serialized = "<value>&lt;</script>\r\u0000</value>"

        val rendered = renderDocument(serialized)

        assertContains(
            rendered,
            "&lt;value>&amp;lt;&lt;/script>&#13;&#0;&lt;/value>",
        )
        assertFalse("</script>\r\u0000" in rendered)
    }

    // Renders a page without a root to verify that static documents ship no hydration state.
    @Test
    fun allowsAStaticDocumentWithoutShippingHydrationState() {
        val rendered = renderHydratedDocument {
            Html {
                Head { Title { Text("Static page") } }
                Body { Text("Nothing to hydrate") }
            }
        }

        assertEquals(
            "<!doctype html><html><head><title>Static page</title></head>" +
                "<body>Nothing to hydrate</body></html>",
            rendered,
        )
        assertFalse("data-compose-hydration-state" in rendered)
    }

    // Renders invalid top-level shapes to verify that only one html element is accepted.
    @Test
    fun rejectsFragmentsAndMultipleHtmlElements() {
        val fragmentFailure = assertFailsWith<IllegalArgumentException> {
            renderHydratedDocument { Div() }
        }
        val multipleDocumentsFailure = assertFailsWith<IllegalArgumentException> {
            renderHydratedDocument {
                Html()
                Html()
            }
        }

        assertContains(fragmentFailure.message.orEmpty(), "exactly one html element")
        assertContains(multipleDocumentsFailure.message.orEmpty(), "exactly one html element")
    }

    // Calls HydrationRoot through the ordinary renderer to verify the context error is clear.
    @Test
    fun rejectsHydrationRootOutsideTheDocumentRenderer() {
        val failure = assertFailsWith<IllegalStateException> {
            composeHtmlToString {
                HydrationRoot(Unit, { "null" }) {}
            }
        }

        assertContains(failure.message.orEmpty(), "inside renderHydratedDocument")
    }

    // Attempts a reserved root attribute to verify that collisions fail explicitly.
    @Test
    fun rejectsRootAttributesOwnedByTheProtocol() {
        val rootFailure = assertFailsWith<IllegalArgumentException> {
            renderHydratedDocument {
                Html {
                    Body {
                        HydrationRoot(
                            initialState = Unit,
                            serializeState = { "null" },
                            rootAttrs = { attr("DATA-COMPOSE-HYDRATION-ROOT", "custom") },
                        ) {}
                    }
                }
            }
        }

        assertTrue(rootFailure.message.orEmpty().contains("owned", ignoreCase = true))
    }

    // Attempts fake protocol markers on ordinary elements, including browser-normalized casing.
    @Test
    fun rejectsProtocolAttributesOnOrdinaryElementsCaseInsensitively() {
        HydrationProtocolAttributes.forEach { attribute ->
            val failure = assertFailsWith<IllegalArgumentException>(attribute) {
                renderHydratedDocument {
                    Html {
                        Body {
                            Div(attrs = { attr(attribute.uppercase(), "") })
                            HydrationRoot(Unit, { "state" }) {}
                        }
                    }
                }
            }

            assertTrue(failure.message.orEmpty().contains("owned", ignoreCase = true))
        }
    }

    @OptIn(org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class)
    @Test
    fun protocolAttributesCannotBeOverwrittenAfterRegistration() {
        val protocolAttrs = AttrsScopeBuilder<HTMLDivElement>().apply {
            hydrationProtocolAttr(HydrationRootAttribute, "")
        }

        val attrFailure = assertFailsWith<IllegalArgumentException> {
            protocolAttrs.attr(HydrationRootAttribute.uppercase(), "custom")
        }
        assertTrue(attrFailure.message.orEmpty().contains("owned", ignoreCase = true))

        val ordinaryAttrs = AttrsScopeBuilder<HTMLDivElement>().apply {
            attr(HydrationRootAttribute, "custom")
        }
        val copyFailure = assertFailsWith<IllegalArgumentException> {
            protocolAttrs.copyFrom(ordinaryAttrs)
        }
        assertTrue(copyFailure.message.orEmpty().contains("owned", ignoreCase = true))
    }

    private fun renderDocument(serializedState: String): String =
        renderHydratedDocument {
            Html {
                Body {
                    HydrationRoot(serializedState, { it }) {}
                }
            }
        }

    private data class TestState(
        val label: String,
        val count: Int,
    ) {
        fun toJson(): String = """{"label":"$label","count":$count}"""
    }
}
