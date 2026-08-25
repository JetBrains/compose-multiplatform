/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

import kotlinx.browser.JsDouble
import kotlinx.browser.JsString
import kotlinx.browser.document
import kotlinx.browser.toJsArray
import kotlinx.browser.toJsDouble
import kotlinx.browser.window
import kotlinx.browser.dom.Audio
import kotlinx.browser.dom.CanvasRenderingContext2D
import kotlinx.browser.dom.HTMLAnchorElement
import kotlinx.browser.dom.HTMLAppletElement
import kotlinx.browser.dom.HTMLBaseElement
import kotlinx.browser.dom.HTMLBodyElement
import kotlinx.browser.dom.HTMLButtonElement
import kotlinx.browser.dom.HTMLCanvasElement
import kotlinx.browser.dom.HTMLDataElement
import kotlinx.browser.dom.HTMLDetailsElement
import kotlinx.browser.dom.HTMLDialogElement
import kotlinx.browser.dom.HTMLDirectoryElement
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.HTMLFontElement
import kotlinx.browser.dom.HTMLFormElement
import kotlinx.browser.dom.HTMLFrameElement
import kotlinx.browser.dom.HTMLFrameSetElement
import kotlinx.browser.dom.HTMLHtmlElement
import kotlinx.browser.dom.HTMLKeygenElement
import kotlinx.browser.dom.HTMLLinkElement
import kotlinx.browser.dom.HTMLMarqueeElement
import kotlinx.browser.dom.HTMLMenuItemElement
import kotlinx.browser.dom.HTMLMetaElement
import kotlinx.browser.dom.HTMLModElement
import kotlinx.browser.dom.HTMLQuoteElement
import kotlinx.browser.dom.HTMLScriptElement
import kotlinx.browser.dom.HTMLTemplateElement
import kotlinx.browser.dom.HTMLTimeElement
import kotlinx.browser.dom.HTMLTitleElement
import kotlinx.browser.dom.HTMLUnknownElement
import kotlinx.browser.dom.HTMLVideoElement
import kotlinx.browser.dom.Image
import kotlinx.browser.dom.MutationObserver
import kotlinx.browser.dom.Option
import kotlinx.browser.dom.Path2D
import kotlinx.browser.dom.RadioNodeList
import kotlinx.browser.dom.TouchEvent
import kotlinx.browser.dom.events.CompositionEvent
import kotlinx.browser.dom.events.EventListener
import kotlinx.browser.dom.events.FocusEvent
import kotlinx.browser.dom.events.InputEvent
import kotlinx.browser.dom.events.KeyboardEvent
import kotlinx.browser.dom.events.MouseEvent
import kotlinx.browser.dom.events.WheelEvent
import kotlinx.browser.dom.css.surfaces.exerciseGeneratedCssStyleDeclaration
import kotlinx.browser.dom.css.surfaces.exerciseGeneratedLegacyCssStyleDeclaration
import kotlinx.browser.dom.surfaces.constructCanvasTypes
import kotlinx.browser.dom.surfaces.constructEventSubtypes
import kotlinx.browser.dom.surfaces.constructRelatedEvent
import kotlinx.browser.dom.surfaces.constructWorkerAndNetworkTypes
import kotlinx.browser.dom.surfaces.eventSubtypeHierarchy
import kotlinx.browser.dom.surfaces.useCanvasSurface
import kotlinx.browser.dom.surfaces.useNumericCanvasSequence
import org.w3c.fetch.OMIT
import org.w3c.fetch.RequestCredentials as BrowserRequestCredentials
import kotlin.js.unsafeCast
import kotlin.test.Test

class GeneratedBrowserRuntimeTest {
    @Test
    fun callbacksAndSignatureClosureAreCallable() {
        val root = element<HTMLDivElement>("div")
        val listener = eventListener()
        val observer = MutationObserver { _, _ -> }
        val filter = emptyList<JsString>().toJsArray()

        exerciseGeneratedCallbacks(root, window, document, root, listener, observer, filter)
        exerciseGeneratedSignatureClosure(root, listener, root)
    }

    @Test
    fun hierarchyAndCssMembersAreCallable() {
        val div = element<HTMLDivElement>("div")

        exerciseGeneratedHierarchy(
            div = div,
            child = document.createTextNode("portable"),
            anchor = element<HTMLAnchorElement>("a"),
            button = element<HTMLButtonElement>("button"),
            form = element<HTMLFormElement>("form"),
            video = element<HTMLVideoElement>("video"),
            listener = eventListener(),
            composition = CompositionEvent("compositionstart"),
            focus = FocusEvent("focus"),
            input = InputEvent("input"),
            keyboard = KeyboardEvent("keydown"),
            mouse = MouseEvent("click"),
            wheel = WheelEvent("wheel"),
        )
        exerciseGeneratedCssStyleDeclaration(div)
        if (hasLegacyCssMembers(div)) exerciseGeneratedLegacyCssStyleDeclaration(div.style)
    }

    @Test
    fun htmlElementsAreCallable() {
        val dialog = element<HTMLDialogElement>("dialog")
        val form = element<HTMLFormElement>("form")
        val radio = radioNodeList(form)
        document.body?.appendChild(dialog)

        try {
            exerciseGeneratedHtmlElements(
                audio = Audio(),
                applet = element<HTMLAppletElement>("applet"),
                base = element<HTMLBaseElement>("base"),
                body = element<HTMLBodyElement>("body"),
                data = element<HTMLDataElement>("data"),
                details = element<HTMLDetailsElement>("details"),
                dialog = dialog,
                directory = element<HTMLDirectoryElement>("dir"),
                font = element<HTMLFontElement>("font"),
                frame = element<HTMLFrameElement>("frame"),
                frameset = element<HTMLFrameSetElement>("frameset"),
                html = element<HTMLHtmlElement>("html"),
                keygen = element<HTMLKeygenElement>("keygen"),
                link = element<HTMLLinkElement>("link"),
                marquee = element<HTMLMarqueeElement>("marquee"),
                menuItem = element<HTMLMenuItemElement>("menuitem"),
                meta = element<HTMLMetaElement>("meta"),
                mod = element<HTMLModElement>("ins"),
                quote = element<HTMLQuoteElement>("q"),
                script = element<HTMLScriptElement>("script"),
                template = element<HTMLTemplateElement>("template"),
                time = element<HTMLTimeElement>("time"),
                title = element<HTMLTitleElement>("title"),
                unknown = element<HTMLUnknownElement>("portable-unknown"),
                image = Image(),
                option = Option(),
                radio = radio,
            )
        } finally {
            dialog.remove()
        }
    }

    @Test
    fun canvasAndEventTypesAreCallable() {
        val canvas = element<HTMLCanvasElement>("canvas")
        val context = canvasContext(canvas)
        val values = listOf(1.0.toJsDouble(), 2.0.toJsDouble()).toJsArray<JsDouble>()

        constructCanvasTypes()
        useCanvasSurface(context, canvas, Path2D())
        useNumericCanvasSequence(context, values)
        constructWorkerAndNetworkTypes(BrowserRequestCredentials.OMIT)
        constructEventSubtypes()
        if (hasRelatedEvent()) constructRelatedEvent()
        eventSubtypeHierarchy(
            close = kotlinx.browser.dom.CloseEvent("close"),
            custom = kotlinx.browser.dom.CustomEvent("custom"),
            error = kotlinx.browser.dom.ErrorEvent("error"),
            media = kotlinx.browser.dom.MediaQueryListEvent("change"),
            related = relatedEvent(),
            touch = touchEvent(),
        )
    }
}

private inline fun <reified T : kotlin.js.JsAny> element(name: String): T =
    document.createElement(name).unsafeCast<T>()

private fun eventListener(): EventListener = js("({ handleEvent: function() {} })")

private fun touchEvent(): TouchEvent = js("new TouchEvent('touch')")

private fun relatedEvent(): kotlinx.browser.dom.RelatedEvent = js("new Event('related')")

private fun canvasContext(canvas: HTMLCanvasElement): CanvasRenderingContext2D =
    js("canvas.getContext('2d')")

private fun hasLegacyCssMembers(element: HTMLDivElement): Boolean =
    js("typeof element.style.setPropertyValue === 'function' && " +
        "typeof element.style.setPropertyPriority === 'function'")

private fun hasRelatedEvent(): Boolean = js("typeof RelatedEvent === 'function'")

private fun radioNodeList(form: HTMLFormElement): RadioNodeList {
    repeat(2) {
        val radio = document.createElement("input")
        radio.setAttribute("type", "radio")
        radio.setAttribute("name", "portable")
        form.appendChild(radio)
    }
    return form.elements.namedItem("portable")!!.unsafeCast<RadioNodeList>()
}
