package org.jetbrains.compose.web.core.tests

import androidx.compose.web.css.AlignContent
import androidx.compose.web.css.AlignItems
import androidx.compose.web.css.AlignSelf
import androidx.compose.web.css.Color
import androidx.compose.web.css.DisplayStyle
import androidx.compose.web.css.FlexDirection
import androidx.compose.web.css.FlexWrap
import androidx.compose.web.css.JustifyContent
import androidx.compose.web.css.Position
import androidx.compose.web.css.alignContent
import androidx.compose.web.css.alignItems
import androidx.compose.web.css.alignSelf
import androidx.compose.web.css.border
import androidx.compose.web.css.borderRadius
import androidx.compose.web.css.bottom
import androidx.compose.web.css.color
import androidx.compose.web.css.display
import androidx.compose.web.css.flexDirection
import androidx.compose.web.css.flexFlow
import androidx.compose.web.css.flexGrow
import androidx.compose.web.css.flexShrink
import androidx.compose.web.css.flexWrap
import androidx.compose.web.css.height
import androidx.compose.web.css.justifyContent
import androidx.compose.web.css.left
import androidx.compose.web.css.opacity
import androidx.compose.web.css.order
import androidx.compose.web.css.position
import androidx.compose.web.css.px
import androidx.compose.web.css.right
import androidx.compose.web.css.top
import androidx.compose.web.css.value
import androidx.compose.web.css.width
import androidx.compose.web.elements.Div
import androidx.compose.web.elements.Span
import androidx.compose.web.elements.Text
import androidx.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticComposableTests {
    @Test
    fun emptyComposable() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {}
        assertEquals("<div></div>", root.outerHTML)
    }

    @Test
    fun textChild() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Text("inner text")
        }
        assertEquals("<div>inner text</div>", root.outerHTML)
    }

    @Test
    fun attrs() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                attrs = {
                    classes("some", "simple", "classes")
                    id("special")
                    attr("data-val", "some data")
                    attr("data-val", "some other data")
                    id("verySpecial")
                }
            ) {}
        }

        val el = root.firstChild
        assertTrue(el is HTMLElement, "element not found")

        assertEquals("verySpecial", el.getAttribute("id"))
        assertEquals("some simple classes", el.getAttribute("class"))
        assertEquals("some other data", el.getAttribute("data-val"))
    }

    @Test
    fun styles() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        opacity(0.3)
                        color("red")
                        opacity(0.2)
                        color("green")
                    }
                }
            ) {}
        }

        assertEquals("opacity: 0.2; color: green;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesBorder() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        property("border", value("1px solid red"))
                    }
                }
            ) {}
            Div(
                {
                    style {
                        border(3.px, color = Color("green"))
                    }
                }
            ) {}
        }

        assertEquals("border: 1px solid red;", (root.children[0] as HTMLElement).style.cssText)
        root.children[1]?.let {
            val el = it.unsafeCast<HTMLElement>()
            assertEquals(
                "green",
                el.style.getPropertyValue("border-color")
            )
            assertEquals(
                "3px",
                el.style.getPropertyValue("border-width"),
            )
        }
    }

    @Test
    fun stylesOrder() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        order(-4)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        order(3)
                    }
                }
            ) {}
        }

        assertEquals("order: -4;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("order: 3;", (root.children[1] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesFlexGrow() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        flexGrow(3)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        flexGrow(2.5)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        flexGrow(1e2)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        flexGrow(.6)
                    }
                }
            ) {}
        }

        assertEquals("flex-grow: 3;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("flex-grow: 2.5;", (root.children[1] as HTMLElement).style.cssText)
        assertEquals("flex-grow: 100;", (root.children[2] as HTMLElement).style.cssText)
        assertEquals("flex-grow: 0.6;", (root.children[3] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesFlexShrink() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        flexShrink(3)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        flexShrink(2.5)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        flexShrink(1e2)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        flexShrink(.6)
                    }
                }
            ) {}
        }

        assertEquals("flex-shrink: 3;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("flex-shrink: 2.5;", (root.children[1] as HTMLElement).style.cssText)
        assertEquals("flex-shrink: 100;", (root.children[2] as HTMLElement).style.cssText)
        assertEquals("flex-shrink: 0.6;", (root.children[3] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesWidth() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        width(100.px)
                    }
                }
            ) {}
        }

        assertEquals("width: 100px;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesBorderRadius() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        borderRadius(3.px)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        borderRadius(3.px, 5.px)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        borderRadius(3.px, 5.px, 4.px)
                    }
                }
            ) {}
            Div(
                {
                    style {
                        borderRadius(3.px, 5.px, 4.px, 1.px)
                    }
                }
            ) {}
        }

        assertEquals("border-radius: 3px;", (root.children[0] as HTMLElement).style.cssText)
        assertEquals("border-radius: 3px 5px;", (root.children[1] as HTMLElement).style.cssText)
        assertEquals("border-radius: 3px 5px 4px;", (root.children[2] as HTMLElement).style.cssText)
        assertEquals(
            "border-radius: 3px 5px 4px 1px;",
            (root.children[3] as HTMLElement).style.cssText
        )
    }

    @Test
    fun stylesTop() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        top(100.px)
                    }
                }
            ) {}
        }

        assertEquals("top: 100px;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesBottom() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        bottom(100.px)
                    }
                }
            ) {}
        }

        assertEquals("bottom: 100px;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesLeft() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        left(100.px)
                    }
                }
            ) {}
        }

        assertEquals("left: 100px;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesRight() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        right(100.px)
                    }
                }
            ) {}
        }

        assertEquals("right: 100px;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesHeight() {
        val root = "div".asHtmlElement()
        renderComposable(
            root = root
        ) {
            Div(
                {
                    style {
                        height(100.px)
                    }
                }
            ) {}
        }

        assertEquals("height: 100px;", (root.children[0] as HTMLElement).style.cssText)
    }

    @Test
    fun stylesDisplay() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<DisplayStyle>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { displayStyle ->
                Div(
                    {
                        style {
                            display(displayStyle)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, displayStyle ->
            assertEquals(
                "display: ${displayStyle.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesFlexDirection() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<FlexDirection>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { flexDirection ->
                Span(
                    {
                        style {
                            flexDirection(flexDirection)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, displayStyle ->
            assertEquals(
                "flex-direction: ${displayStyle.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesFlexWrap() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<FlexWrap>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { flexWrap ->
                Span(
                    {
                        style {
                            flexWrap(flexWrap)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, displayStyle ->
            assertEquals(
                "flex-wrap: ${displayStyle.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesFlexFlow() {
        val root = "div".asHtmlElement()
        val flexWraps = enumValues<FlexWrap>()
        val flexDirections = enumValues<FlexDirection>()
        renderComposable(
            root = root
        ) {
            flexDirections.forEach { flexDirection ->
                flexWraps.forEach { flexWrap ->
                    Span(
                        {
                            style {
                                flexFlow(flexDirection, flexWrap)
                            }
                        }
                    ) { }
                }
            }
        }

        flexDirections.forEachIndexed { i, flexDirection ->
            flexWraps.forEachIndexed { j, flexWrap ->
                assertEquals(
                    "flex-flow: ${flexDirection.value} ${flexWrap.value};",
                    (root.children[3 * i + j % 3] as HTMLElement).style.cssText
                )
            }
        }
    }

    @Test
    fun stylesJustifyContent() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<JustifyContent>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { justifyContent ->
                Span(
                    {
                        style {
                            justifyContent(justifyContent)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, justifyContent ->
            assertEquals(
                "justify-content: ${justifyContent.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesAlignSelf() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<AlignSelf>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { alignSelf ->
                Span(
                    {
                        style {
                            alignSelf(alignSelf)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, alignSelf ->
            assertEquals(
                "align-self: ${alignSelf.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesAlignItems() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<AlignItems>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { alignItems ->
                Span(
                    {
                        style {
                            alignItems(alignItems)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, alignItems ->
            assertEquals(
                "align-items: ${alignItems.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesAlignContent() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<AlignContent>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { alignContent ->
                Span(
                    {
                        style {
                            alignContent(alignContent)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, alignContent ->
            assertEquals(
                "align-content: ${alignContent.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }

    @Test
    fun stylesPosition() {
        val root = "div".asHtmlElement()
        val enumValues = enumValues<Position>()
        renderComposable(
            root = root
        ) {
            enumValues.forEach { position ->
                Span(
                    {
                        style {
                            position(position)
                        }
                    }
                ) { }
            }
        }

        enumValues.forEachIndexed { index, position ->
            assertEquals(
                "position: ${position.value};",
                (root.children[index] as HTMLElement).style.cssText
            )
        }
    }
}