/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Provides HTML element fixtures for browser runtime tests.
import kotlinx.browser.dom.Audio
import kotlinx.browser.dom.DocumentFragment
import kotlinx.browser.dom.HTMLAppletElement
import kotlinx.browser.dom.HTMLAudioElement
import kotlinx.browser.dom.HTMLBaseElement
import kotlinx.browser.dom.HTMLBodyElement
import kotlinx.browser.dom.HTMLDataElement
import kotlinx.browser.dom.HTMLDetailsElement
import kotlinx.browser.dom.HTMLDialogElement
import kotlinx.browser.dom.HTMLDirectoryElement
import kotlinx.browser.dom.HTMLElement
import kotlinx.browser.dom.HTMLFontElement
import kotlinx.browser.dom.HTMLFrameElement
import kotlinx.browser.dom.HTMLFrameSetElement
import kotlinx.browser.dom.HTMLHtmlElement
import kotlinx.browser.dom.HTMLKeygenElement
import kotlinx.browser.dom.HTMLLinkElement
import kotlinx.browser.dom.HTMLMarqueeElement
import kotlinx.browser.dom.HTMLMenuItemElement
import kotlinx.browser.dom.HTMLMetaElement
import kotlinx.browser.dom.HTMLModElement
import kotlinx.browser.dom.HTMLOptionElement
import kotlinx.browser.dom.HTMLQuoteElement
import kotlinx.browser.dom.HTMLScriptElement
import kotlinx.browser.dom.HTMLTemplateElement
import kotlinx.browser.dom.HTMLTimeElement
import kotlinx.browser.dom.HTMLTitleElement
import kotlinx.browser.dom.HTMLUnknownElement
import kotlinx.browser.dom.Image
import kotlinx.browser.dom.NodeList
import kotlinx.browser.dom.Option
import kotlinx.browser.dom.RadioNodeList

private fun htmlClassifiersAsElements(
    audio: Audio,
    applet: HTMLAppletElement,
    base: HTMLBaseElement,
    body: HTMLBodyElement,
    data: HTMLDataElement,
    details: HTMLDetailsElement,
    dialog: HTMLDialogElement,
    directory: HTMLDirectoryElement,
    font: HTMLFontElement,
    frame: HTMLFrameElement,
    frameset: HTMLFrameSetElement,
    html: HTMLHtmlElement,
    keygen: HTMLKeygenElement,
    link: HTMLLinkElement,
    marquee: HTMLMarqueeElement,
    menuItem: HTMLMenuItemElement,
    meta: HTMLMetaElement,
    mod: HTMLModElement,
    quote: HTMLQuoteElement,
    script: HTMLScriptElement,
    template: HTMLTemplateElement,
    time: HTMLTimeElement,
    title: HTMLTitleElement,
    unknown: HTMLUnknownElement,
    image: Image,
    option: Option,
): List<HTMLElement> = listOf(
    audio, applet, base, body, data, details, dialog, directory, font, frame, frameset, html,
    keygen, link, marquee, menuItem, meta, mod, quote, script, template, time, title, unknown,
    image, option,
)

private fun buildHtmlConvenienceClasses(): List<HTMLElement> = listOf(
    Audio(),
    Audio("data:audio/wav;base64,"),
    Image(),
    Image(width = 640, height = 480),
    Option(),
    Option(text = "Portable", value = "portable", defaultSelected = true, selected = true),
)

private fun useHtmlElementMembers(
    audio: Audio,
    dialog: HTMLDialogElement,
    link: HTMLLinkElement,
    marquee: HTMLMarqueeElement,
    radio: RadioNodeList,
    template: HTMLTemplateElement,
): DocumentFragment {
    val media: HTMLAudioElement = audio
    media.play()
    dialog.open = true
    dialog.show()
    dialog.close()
    dialog.showModal()
    dialog.close()
    link.href = "https://example.org/style.css"
    link.rel = "stylesheet"
    marquee.start()
    marquee.stop()
    radio.value = "selected"
    val nodes: NodeList = radio
    nodes.item(0)
    return template.content
}

private fun optionKeepsItsElementType(option: Option): HTMLOptionElement = option

internal fun exerciseGeneratedHtmlElements(
    audio: Audio,
    applet: HTMLAppletElement,
    base: HTMLBaseElement,
    body: HTMLBodyElement,
    data: HTMLDataElement,
    details: HTMLDetailsElement,
    dialog: HTMLDialogElement,
    directory: HTMLDirectoryElement,
    font: HTMLFontElement,
    frame: HTMLFrameElement,
    frameset: HTMLFrameSetElement,
    html: HTMLHtmlElement,
    keygen: HTMLKeygenElement,
    link: HTMLLinkElement,
    marquee: HTMLMarqueeElement,
    menuItem: HTMLMenuItemElement,
    meta: HTMLMetaElement,
    mod: HTMLModElement,
    quote: HTMLQuoteElement,
    script: HTMLScriptElement,
    template: HTMLTemplateElement,
    time: HTMLTimeElement,
    title: HTMLTitleElement,
    unknown: HTMLUnknownElement,
    image: Image,
    option: Option,
    radio: RadioNodeList,
) {
    htmlClassifiersAsElements(
        audio, applet, base, body, data, details, dialog, directory, font, frame, frameset, html,
        keygen, link, marquee, menuItem, meta, mod, quote, script, template, time, title, unknown,
        image, option,
    )
    buildHtmlConvenienceClasses()
    useHtmlElementMembers(audio, dialog, link, marquee, radio, template)
    optionKeepsItsElementType(option)
}
