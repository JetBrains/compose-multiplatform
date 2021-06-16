/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.interactions.Actions

class EventTests : BaseIntegrationTests() {

    @Test
    fun `double click updates text`() {
        openTestPage("doubleClickUpdatesText")

        val box = driver.findElement(By.id("box"))

        val actions = Actions(driver)

        actions
            .doubleClick(box)
            .perform()

        waitTextToBe(value = "Double Click Works!", textId = "txt")
    }

    @Test
    fun `focusin and focusout update the text`() {
        openTestPage("focusInAndFocusOutUpdateTheText")

        waitTextToBe(value = "", textId = "txt")

        val input = driver.findElement(By.id("focusableInput"))

        val actions = Actions(driver)

        actions.moveToElement(input)
            .click()
            .perform()

        waitTextToBe(value = "focused", textId = "txt")

        val actions2 = Actions(driver)

        actions2.moveToElement(driver.findElement(By.id("txt")))
            .click()
            .perform()

        waitTextToBe(value = "not focused", textId = "txt")
    }

    @Test
    fun `focus and blur update the text`() {
        openTestPage("focusAndBlurUpdateTheText")

        waitTextToBe(value = "", textId = "txt")

        val input = driver.findElement(By.id("focusableInput"))

        val actions = Actions(driver)

        actions.moveToElement(input)
            .click()
            .perform()

        waitTextToBe(value = "focused", textId = "txt")

        val actions2 = Actions(driver)

        actions2.moveToElement(driver.findElement(By.id("txt")))
            .click()
            .perform()

        waitTextToBe(value = "blured", textId = "txt")
    }

    @Test
    fun `scroll updates the text`() {
        openTestPage("scrollUpdatesText")

        waitTextToBe(value = "", textId = "txt")

        val box = driver.findElement(By.id("box"))

        val actions = Actions(driver)
        actions.moveToElement(box)
            .click()
            .sendKeys(Keys.ARROW_DOWN)
            .perform()

        waitTextToBe(value = "Scrolled", textId = "txt")
    }

    @Test
    fun `select event update the txt`() {
        openTestPage("selectEventUpdatesText")
        waitTextToBe(value = "None")

        val selectableText = driver.findElement(By.id("selectableText"))

        val action = Actions(driver)

        action.moveToElement(selectableText,3,3)
            .click().keyDown(Keys.SHIFT)
            .moveToElement(selectableText,200, 0)
            .click().keyUp(Keys.SHIFT)
            .build()
            .perform()

        waitTextToBe(value = "Text Selected")
    }
}
