/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Verifies JVM behavior for generated HTML element constructors and mutable members.
import kotlinx.browser.dom.Audio
import kotlinx.browser.dom.HTMLAudioElement
import kotlinx.browser.dom.HTMLDialogElement
import kotlinx.browser.dom.HTMLImageElement
import kotlinx.browser.dom.HTMLOptionElement
import kotlinx.browser.dom.Image
import kotlinx.browser.dom.NodeList
import kotlinx.browser.dom.Option
import kotlinx.browser.dom.RadioNodeList
import kotlinx.browser.dom.UnionElementOrRadioNodeList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeneratedHtmlElementJvmTest {
    @Test
    fun convenienceConstructorsStoreThePropertiesTheyInitialize() {
        val audio = Audio("track.ogg")
        val image = Image(width = 640, height = 480)
        val option = Option(
            text = "Common",
            value = "common",
            defaultSelected = true,
            selected = true,
        )

        assertIs<HTMLAudioElement>(audio)
        assertEquals("track.ogg", audio.src)
        assertIs<HTMLImageElement>(image)
        assertEquals(640, image.width)
        assertEquals(480, image.height)
        assertIs<HTMLOptionElement>(option)
        assertEquals("Common", option.text)
        assertEquals("common", option.value)
        assertEquals(true, option.defaultSelected)
        assertEquals(true, option.selected)
    }

    @Test
    fun newlySelectedAbstractStubsKeepMutableMembersAndHierarchy() {
        val dialog = TestDialog()
        dialog.open = true
        dialog.returnValue = "accepted"
        dialog.show()
        dialog.showModal()
        dialog.close()

        assertEquals(true, dialog.open)
        assertEquals("accepted", dialog.returnValue)

        val radio = TestRadioNodeList()
        radio.value = "choice"

        assertEquals("choice", radio.value)
        assertIs<NodeList>(radio)
        assertIs<UnionElementOrRadioNodeList>(radio)
    }

    private class TestDialog : HTMLDialogElement()

    private class TestRadioNodeList : RadioNodeList()
}
