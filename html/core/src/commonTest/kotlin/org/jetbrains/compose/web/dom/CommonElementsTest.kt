package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.attributes.ARel
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.LinkRel
import org.jetbrains.compose.web.attributes.ScriptType
import org.jetbrains.compose.web.attributes.href
import org.jetbrains.compose.web.attributes.rel
import org.jetbrains.compose.web.attributes.src
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.composeHtmlToString
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonElementsTest {
    @Test
    fun rendersDocumentElements() {
        val html = composeHtmlToString {
            Html({ lang("en") }) {
                Head {
                    Meta { attr("charset", "UTF-8") }
                    Meta {
                        attr("name", "viewport")
                        attr("content", "width=device-width, initial-scale=1")
                    }
                    Title { Text("Compose HTML") }
                    Link {
                        rel(LinkRel.Alternate, LinkRel.Stylesheet)
                        href("styles.css")
                        type("text/css")
                    }
                }
                Body {
                    Header {
                        H1 { Text("String rendering") }
                    }
                }
            }
        }

        assertEquals(
            "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<title>Compose HTML</title>" +
                "<link rel=\"alternate stylesheet\" href=\"styles.css\" type=\"text/css\">" +
                "</head>" +
                "<body><header><h1>String rendering</h1></header></body>" +
                "</html>",
            html,
        )
    }

    @Test
    fun rendersExternalScript() {
        val html = composeHtmlToString {
            Script {
                src("/app.js")
                type(ScriptType.Module)
            }
        }

        assertEquals(
            "<script src=\"/app.js\" type=\"module\"></script>",
            html,
        )
    }

    @Test
    fun rendersSemanticAndTextElements() {
        val html = composeHtmlToString {
            Address { Text("address") }
            Article({ id("article") }) { Text("article") }
            Aside { Text("aside") }
            Header { Text("header") }
            Section { Text("section") }
            Nav { Text("nav") }
            Main { Text("main") }
            Footer { Text("footer") }
            H1 { Text("h1") }
            H2 { Text("h2") }
            H3 { Text("h3") }
            H4 { Text("h4") }
            H5 { Text("h5") }
            H6 { Text("h6") }
            P { Text("p") }
            Em { Text("em") }
            I { Text("i") }
            B { Text("b") }
            Small { Text("small") }
            Sup { Text("sup") }
            Sub { Text("sub") }
            Blockquote { Text("blockquote") }
            Pre { Text("pre") }
            Code { Text("code") }
        }

        assertEquals(
            "<address>address</address>" +
                "<article id=\"article\">article</article>" +
                "<aside>aside</aside>" +
                "<header>header</header>" +
                "<section>section</section>" +
                "<nav>nav</nav>" +
                "<main>main</main>" +
                "<footer>footer</footer>" +
                "<h1>h1</h1>" +
                "<h2>h2</h2>" +
                "<h3>h3</h3>" +
                "<h4>h4</h4>" +
                "<h5>h5</h5>" +
                "<h6>h6</h6>" +
                "<p>p</p>" +
                "<em>em</em>" +
                "<i>i</i>" +
                "<b>b</b>" +
                "<small>small</small>" +
                "<sup>sup</sup>" +
                "<sub>sub</sub>" +
                "<blockquote>blockquote</blockquote>" +
                "<pre>pre</pre>" +
                "<code>code</code>",
            html,
        )
    }

    @Test
    fun rendersListElements() {
        val html = composeHtmlToString {
            Ul { Li { Text("unordered") } }
            Ol { Li { Text("ordered") } }
            DList {
                DTerm { Text("term") }
                DDescription { Text("description") }
            }
        }

        assertEquals(
            "<ul><li>unordered</li></ul>" +
                "<ol><li>ordered</li></ol>" +
                "<dl><dt>term</dt><dd>description</dd></dl>",
            html,
        )
    }

    @Test
    fun rendersMediaAndOtherContainerElements() {
        val html = composeHtmlToString {
            Audio { Text("audio") }
            Video { Text("video") }
            Picture { Text("picture") }
            Canvas { Text("canvas") }
            HTMLMap { Text("map") }
            Datalist { Text("datalist") }
            Fieldset { Legend { Text("legend") } }
            Meter { Text("meter") }
            Output { Text("output") }
            Progress { Text("progress") }
            Iframe { Text("iframe") }
            Object { Text("object") }
        }

        assertEquals(
            "<audio>audio</audio>" +
                "<video>video</video>" +
                "<picture>picture</picture>" +
                "<canvas>canvas</canvas>" +
                "<map>map</map>" +
                "<datalist>datalist</datalist>" +
                "<fieldset><legend>legend</legend></fieldset>" +
                "<meter>meter</meter>" +
                "<output>output</output>" +
                "<progress>progress</progress>" +
                "<iframe>iframe</iframe>" +
                "<object>object</object>",
            html,
        )
    }

    @Test
    fun rendersTableElementsAndButton() {
        val html = composeHtmlToString {
            Button { Text("button") }
            Table {
                Caption { Text("caption") }
                Colgroup()
                Thead {
                    Tr { Th { Text("heading") } }
                }
                Tbody {
                    Tr { Td { Text("body") } }
                }
                Tfoot {
                    Tr { Td { Text("footer") } }
                }
            }
        }

        assertEquals(
            "<button>button</button>" +
                "<table>" +
                "<caption>caption</caption>" +
                "<colgroup></colgroup>" +
                "<thead><tr><th>heading</th></tr></thead>" +
                "<tbody><tr><td>body</td></tr></tbody>" +
                "<tfoot><tr><td>footer</td></tr></tfoot>" +
                "</table>",
            html,
        )
    }

    @Test
    fun rendersLinkedImageAndFormElements() {
        val html = composeHtmlToString {
            A(
                href = "/docs?part=one&format=html",
                attrs = { rel(ARel.NoReferrer) },
            ) {
                Text("Documentation")
            }
            Img(
                src = "/images/logo.png",
                alt = "Logo <preview>",
            )
            Form(action = "/submit") {
                Label(forId = "choice") {
                    Text("Choose")
                }
                Select(
                    attrs = { id("choice") },
                    multiple = true,
                ) {
                    Option(value = "one") {
                        Text("One")
                    }
                    OptGroup(label = "More") {
                        Option(value = "two") {
                            Text("Two")
                        }
                    }
                }
            }
        }

        assertEquals(
            "<a href=\"/docs?part=one&amp;format=html\" rel=\"noreferrer\">Documentation</a>" +
                "<img src=\"/images/logo.png\" alt=\"Logo &lt;preview&gt;\">" +
                "<form action=\"/submit\">" +
                "<label for=\"choice\">Choose</label>" +
                "<select multiple id=\"choice\">" +
                "<option value=\"one\">One</option>" +
                "<optgroup label=\"More\"><option value=\"two\">Two</option></optgroup>" +
                "</select>" +
                "</form>",
            html,
        )
    }

    @Test
    fun rendersSimpleVoidElementsWithoutContentOrEndTags() {
        val html = composeHtmlToString {
            Area(attrs = { attr("shape", "rect") }) {
                Text("ignored")
            }
            Track(attrs = { attr("kind", "captions") }) {
                Text("ignored")
            }
            Embed(attrs = { attr("src", "plugin.bin") }) {
                Text("ignored")
            }
            Param(attrs = { attr("name", "quality") }) {
                Text("ignored")
            }
            Source(attrs = { attr("src", "movie.mp4") }) {
                Text("ignored")
            }
            Br { title("break") }
            Hr { id("rule") }
            Col { attr("span", "2") }
        }

        assertEquals(
            "<area shape=\"rect\">" +
                "<track kind=\"captions\">" +
                "<embed src=\"plugin.bin\">" +
                "<param name=\"quality\">" +
                "<source src=\"movie.mp4\">" +
                "<br title=\"break\">" +
                "<hr id=\"rule\">" +
                "<col span=\"2\">",
            html,
        )
    }

    @Test
    fun rendersInputsWithSerializableState() {
        val html = composeHtmlToString {
            Input(InputType.Text) {
                id("message")
                defaultValue("Hello & welcome")
                attr("required", "")
            }
            Input(InputType.Radio) {
                attr("name", "choice")
                value("one")
                defaultChecked()
            }
        }

        assertEquals(
            "<input type=\"text\" id=\"message\" value=\"Hello &amp; welcome\" required>" +
                "<input type=\"radio\" name=\"choice\" value=\"one\" checked>",
            html,
        )
    }

    @Test
    fun skipsControlledInputPropertiesWithoutDomElementAccess() {
        val html = composeHtmlToString {
            Input(InputType.Text) {
                value("controlled")
            }
            Input(InputType.Checkbox) {
                checked(true)
            }
        }

        assertEquals(
            "<input type=\"text\"><input type=\"checkbox\">",
            html,
        )
    }

    @Test
    fun rendersTextAreaAttributesWithoutDomElementAccess() {
        val html = composeHtmlToString {
            TextArea(
                attrs = {
                    id("message")
                    attr("rows", "4")
                    attr("placeholder", "Write <something>")
                }
            )
        }

        assertEquals(
            "<textarea id=\"message\" rows=\"4\" " +
                "placeholder=\"Write &lt;something&gt;\"></textarea>",
            html,
        )
    }

    @Test
    fun skipsTextAreaValuePropertiesWithoutDomElementAccess() {
        val html = composeHtmlToString {
            TextArea(value = "controlled")
            TextArea {
                defaultValue("default")
            }
        }

        assertEquals(
            "<textarea></textarea><textarea></textarea>",
            html,
        )
    }
}
