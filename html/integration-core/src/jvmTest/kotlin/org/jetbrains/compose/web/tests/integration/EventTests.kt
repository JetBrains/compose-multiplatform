/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.touch.TouchActions

class EventTests : BaseIntegrationTests() {

    companion object {
        private val COMMAND_CROSS_PLATFORM = System.getProperty("os.name").lowercase().let { osName ->
            when {
                osName.contains("mac os") -> Keys.COMMAND
                else -> Keys.CONTROL
            }
        }
    }

    @ResolveDrivers
    fun `double click updates text`(driver: WebDriver) {
        driver.openTestPage("doubleClickUpdatesText")

        val box = driver.findElement(By.id("box"))

        val actions = Actions(driver)

        actions
            .doubleClick(box)
            .perform()

        driver.waitTextToBe(value = "Double Click Works!", textId = "txt")
    }

    @ResolveDrivers
    fun `focusin and focusout update the text`(driver: WebDriver) {
        driver.openTestPage("focusInAndFocusOutUpdateTheText")

        driver.waitTextToBe(value = "", textId = "txt")

        val input = driver.findElement(By.id("focusableInput"))

        val actions = Actions(driver)

        actions.moveToElement(input)
            .click()
            .perform()

        driver.waitTextToBe(value = "focused", textId = "txt")

        val actions2 = Actions(driver)

        actions2.moveToElement(driver.findElement(By.id("txt")))
            .click()
            .perform()

        driver.waitTextToBe(value = "not focused", textId = "txt")
    }

    @ResolveDrivers
    fun `focus and blur update the text`(driver: WebDriver) {
        driver.openTestPage("focusAndBlurUpdateTheText")

        driver.waitTextToBe(value = "", textId = "txt")

        val input = driver.findElement(By.id("focusableInput"))

        val actions = Actions(driver)

        actions.moveToElement(input)
            .click()
            .perform()

        driver.waitTextToBe(value = "focused", textId = "txt")

        val actions2 = Actions(driver)

        actions2.moveToElement(driver.findElement(By.id("txt")))
            .click()
            .perform()

        driver.waitTextToBe(value = "blured", textId = "txt")
    }

    @ResolveDrivers
    fun `scroll updates the text`(driver: WebDriver) {
        driver.openTestPage("scrollUpdatesText")

        driver.waitTextToBe(value = "", textId = "txt")

        val box = driver.findElement(By.id("box"))

        val actions = Actions(driver)
        actions.moveToElement(box)
            .click()
            .sendKeys(Keys.ARROW_DOWN)
            .perform()

        driver.waitTextToBe(value = "Scrolled", textId = "txt")
    }

    @ResolveDrivers
    fun `select event update the txt`(driver: WebDriver) {
        driver.openTestPage("selectEventUpdatesText")
        driver.waitTextToBe(value = "None")

        val selectableText = driver.findElement(By.id("selectableText"))

        val selectAll = Keys.chord(COMMAND_CROSS_PLATFORM, "a")
        selectableText.sendKeys(selectAll)

        driver.waitTextToBe(value = "This is a text to be selected")
    }

    @ResolveDrivers
    fun `select event in TextArea update the txt`(driver: WebDriver) {
        driver.openTestPage("selectEventInTextAreaUpdatesText")
        driver.waitTextToBe(value = "None")
        driver.waitTextToBe(value = "None", textId = "txt2")

        val selectableText = driver.findElement(By.id("textArea"))

        val selectAll = Keys.chord(COMMAND_CROSS_PLATFORM, "a")
        selectableText.sendKeys(selectAll)

        val expectedText = "This is a text to be selected"
        driver.waitTextToBe(value = expectedText)
        driver.waitTextToBe(value = "0,${expectedText.length}", textId = "txt2")
    }

    @ResolveDrivers
    fun `stopImmediatePropagation prevents consequent listeners from being called`(driver: WebDriver) {
        driver.openTestPage("stopOnInputImmediatePropagationWorks")
        driver.waitTextToBe(value = "None")

        val checkBox = driver.findElement(By.id("checkbox"))
        val radioButtonToStopImmediatePropagation = driver.findElement(By.id("radioBtn"))

        checkBox.click()
        driver.waitTextToBe(value = "onInput2")

        radioButtonToStopImmediatePropagation.click()
        driver.waitTextToBe(value = "None")

        checkBox.click()
        driver.waitTextToBe(value = "onInput1")
    }

    @ResolveDrivers
    fun `preventDefault works as expected`(driver: WebDriver) {
        driver.openTestPage("preventDefaultWorks")

        driver.waitTextToBe(value = "None")
        driver.waitTextToBe(textId = "txt2", value = "None")

        val checkBox = driver.findElement(By.id("checkbox"))
        checkBox.click()

        driver.waitTextToBe(value = "Clicked but check should be prevented")
        driver.waitTextToBe(textId = "txt2", value = "None")
    }

    @ResolveDrivers
    fun `stopPropagation works as expected`(driver: WebDriver) {
        driver.openTestPage("stopPropagationWorks")

        driver.waitTextToBe(value = "None")
        driver.waitTextToBe(textId = "txt2", value = "None")

        val checkBox = driver.findElement(By.id("checkbox"))
        val radioButtonToStopImmediatePropagation = driver.findElement(By.id("radioBtn"))

        checkBox.click()
        driver.waitTextToBe(value = "childInput")
        driver.waitTextToBe(textId = "txt2", value = "div caught an input")

        radioButtonToStopImmediatePropagation.click()
        driver.waitTextToBe(value = "None")
        driver.waitTextToBe(textId = "txt2", value = "None")

        checkBox.click()
        driver.waitTextToBe(value = "childInput")
        driver.waitTextToBe(textId = "txt2", value = "None")
    }

    @ResolveDrivers
    fun mouseEnterPlusExtraButtonsPressedUpdatesText(driver: WebDriver) {
        driver.openTestPage("mouseEnterPlusExtraButtonsPressedUpdatesText")
        driver.waitTextToBe(value = "None")

        val box = driver.findElement(By.id("box"))

        Actions(driver).moveToElement(box).perform()

        driver.waitTextToBe(value = "ENTERED+")

        Actions(driver).moveByOffset(0, 100)
            .keyDown(Keys.CONTROL)
            .moveToElement(box)
            .keyUp(Keys.CONTROL)
            .perform()

        driver.waitTextToBe(value = "ENTERED+CTRL")

        Actions(driver).moveByOffset(0, 100)
            .keyDown(Keys.SHIFT)
            .moveToElement(box)
            .keyUp(Keys.SHIFT)
            .perform()

        driver.waitTextToBe(value = "ENTERED+SHIFT")

        Actions(driver).moveByOffset(0, 100)
            .keyDown(Keys.ALT)
            .moveToElement(box)
            .keyUp(Keys.ALT)
            .perform()

        driver.waitTextToBe(value = "ENTERED+ALT")
    }

    @ResolveDrivers
    fun onMouseContextMenuUpdatesText(driver: WebDriver) {
        driver.openTestPage("onMouseContextMenuUpdatesText")
        driver.waitTextToBe(value = "None")

        val box = driver.findElement(By.id("box"))

        Actions(driver).contextClick(box).perform()

        driver.waitTextToBe(value = "MOUSE CONTEXT MENU")
    }

    @ResolveDrivers
    fun displayMouseCoordinates(driver: WebDriver) {
        driver.openTestPage("displayMouseCoordinates")
        driver.waitTextToBe(value = "None")

        val box = driver.findElement(By.id("box"))

        Actions(driver).moveToElement(box).perform()
        driver.waitTextToBe(value = "108,108|100,100")

        Actions(driver).moveToElement(box).moveByOffset(-20, -20).perform()
        driver.waitTextToBe(value = "88,88|80,80")
    }

    private fun WebDriver.copyElementTextToClipboard(element: WebElement) {
        Actions(this)
            .doubleClick(element)
            .keyDown(COMMAND_CROSS_PLATFORM)
            .sendKeys("C")
            .keyUp(COMMAND_CROSS_PLATFORM)
            .perform()
    }

    private fun WebDriver.cutElementTextToClipboard(element: WebElement) {
        Actions(this)
            .doubleClick(element)
            .keyDown(COMMAND_CROSS_PLATFORM)
            .sendKeys("X")
            .keyUp(COMMAND_CROSS_PLATFORM)
            .perform()
    }

    private fun WebDriver.pasteFromClipboardInto(element: WebElement) {
        Actions(this)
            .click(element)
            .keyDown(COMMAND_CROSS_PLATFORM)
            .sendKeys("V")
            .keyUp(COMMAND_CROSS_PLATFORM)
            .perform()
    }

    @ResolveDrivers
    fun copyPasteOverriddenEventsTest(driver: WebDriver) {
        driver.openTestPage("copyPasteEventsTest")
        driver.waitTextToBe(value = "None")

        val textToCopy1 = driver.findElement(By.id("txt_to_copy"))
        val textinput1 = driver.findElement(By.id("textinput"))

        driver.copyElementTextToClipboard(textToCopy1)
        driver.pasteFromClipboardInto(textinput1)

        // Copied text should be changed by the onCopy handler
        driver.waitTextToBe(value = "COPIED_TEXT_WAS_OVERRIDDEN".lowercase())
    }

    @ResolveDrivers
    fun copyPasteUnchangedEventsTest(driver: WebDriver) {
        driver.openTestPage("copyPasteEventsTest")
        driver.waitTextToBe(value = "None")

        val textinput2 = driver.findElement(By.id("textinput"))
        val textToCopy2 = driver.findElement(By.id("txt_to_copy2"))

        driver.copyElementTextToClipboard(textToCopy2)
        driver.pasteFromClipboardInto(textinput2)

        // Copied text should not be changed by the onCopy handler
        driver.waitTextToBe(value = "SomeTestTextToCopy2".lowercase())
    }

    @ResolveDrivers
    fun cutPasteEventsTests(driver: WebDriver) {
        driver.openTestPage("cutPasteEventsTest")
        driver.waitTextToBe(value = "None")

        val textinput1 = driver.findElement(By.id("textinput1"))
        val textinput2 = driver.findElement(By.id("textinput2"))

        driver.cutElementTextToClipboard(textinput1)
        driver.waitTextToBe(value = "Text was cut")

        driver.pasteFromClipboardInto(textinput2)
        driver.waitTextToBe(value = "Modified pasted text = TextToCut")
    }

    @ResolveDrivers
    fun keyDownKeyUpTest(driver: WebDriver) {
        driver.openTestPage("keyDownKeyUpTest")

        driver.waitTextToBe(value = "None", textId = "txt_down")
        driver.waitTextToBe(value = "None", textId = "txt_up")

        val input = driver.findElement(By.id("textinput"))
        input.sendKeys("a")

        driver.waitTextToBe(value = "keydown = a", textId = "txt_down")
        driver.waitTextToBe(value = "keyup = a", textId = "txt_up")

        Actions(driver).keyDown(input, Keys.SHIFT).perform()
        driver.waitTextToBe(value = "keydown = Shift", textId = "txt_down")
        driver.waitTextToBe(value = "keyup = a", textId = "txt_up")

        Actions(driver).keyUp(input, Keys.SHIFT).perform()
        driver.waitTextToBe(value = "keydown = Shift", textId = "txt_down")
        driver.waitTextToBe(value = "keyup = Shift", textId = "txt_up")
    }

    //@ResolveDrivers
    // TODO: at least chrome driver has mobile emulation, so we can try it out (didn't work right away)
    fun touchEventsDispatched(driver: WebDriver) {
        driver.openTestPage("touchEventsDispatched")
        driver.waitTextToBe(value = "None", textId = "txt_start")
        driver.waitTextToBe(value = "None", textId = "txt_move")
        driver.waitTextToBe(value = "None", textId = "txt_end")

        val box = driver.findElement(By.id("box"))

        val boxMiddleX = box.rect.x + box.rect.width / 2
        val boxMiddleY = box.rect.y + box.rect.height / 2

        val boxRightBottomX = box.rect.x + box.rect.width
        val boxRightBottomY = box.rect.y + box.rect.height

        TouchActions(driver)
            .down(boxMiddleX, boxMiddleY)
            .move(boxRightBottomX, boxRightBottomY)
            .up(boxRightBottomX, boxRightBottomY)
            .perform()

        driver.waitTextToBe(value = "STARTED", textId = "txt_start")
        driver.waitTextToBe(value = "MOVED", textId = "txt_move")
        driver.waitTextToBe(value = "ENDED", textId = "txt_end")
    }

    @ResolveDrivers
    fun animationEventsDispatched(driver: WebDriver) {
        driver.openTestPage("animationEventsDispatched")
        driver.waitTextToBe(value = "None", textId = "txt_start")
        driver.waitTextToBe(value = "None", textId = "txt_end")

        driver.findElement(By.id("box")).click()

        driver.waitTextToBe(value = "STARTED - AppStyleSheetWithAnimation-bounce", textId = "txt_start")
        driver.waitTextToBe(value = "ENDED", textId = "txt_end")
    }

    @ResolveDrivers
    fun onSubmitEventForFormDispatched(driver: WebDriver) {
        driver.openTestPage("onSubmitEventForFormDispatched")
        driver.waitTextToBe(value = "None")

        driver.findElement(By.id("send_form_btn")).click()

        driver.waitTextToBe(value = "Form submitted")
    }

    @ResolveDrivers
    fun onResetEventForFormDispatched(driver: WebDriver) {
        driver.openTestPage("onResetEventForFormDispatched")
        driver.waitTextToBe(value = "None")

        driver.findElement(By.id("reset_form_btn")).click()

        driver.waitTextToBe(value = "Form reset")
    }
}
