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

class EventTests : BaseIntegrationTests() {

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

        val action = Actions(driver)

        action.moveToElement(selectableText,3,3)
            .click().keyDown(Keys.SHIFT)
            .moveToElement(selectableText,200, 0)
            .click().keyUp(Keys.SHIFT)
            .build()
            .perform()

        driver.waitTextToBe(value = "Text Selected")
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
}
