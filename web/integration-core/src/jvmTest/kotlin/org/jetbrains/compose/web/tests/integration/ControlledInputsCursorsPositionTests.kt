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
import org.openqa.selenium.WebDriver

class ControlledInputsCursorsPositionTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun textInputTypingIntoMiddle(driver: WebDriver) {
        driver.openTestPage("textInputTypingIntoMiddle")
        driver.waitTextToBe(value = "None")

        val controlledTextInput = driver.findElement(By.id("textInput"))

        controlledTextInput.sendKeys("A")
        driver.waitTextToBe(value = "A")
        controlledTextInput.sendKeys("B")
        driver.waitTextToBe(value = "AB")
        controlledTextInput.sendKeys("C")
        driver.waitTextToBe(value = "ABC")

        controlledTextInput.sendKeys(Keys.ARROW_LEFT)
        controlledTextInput.sendKeys(Keys.ARROW_LEFT)
        assert(driver.cursorPosition("textInput") == 1)

        controlledTextInput.sendKeys("1")
        driver.waitTextToBe(value = "A1BC")

        assert(driver.cursorPosition("textInput") == 2)

        controlledTextInput.sendKeys("2")
        driver.waitTextToBe(value = "A12BC")

        assert(driver.cursorPosition("textInput") == 3)
    }

    @ResolveDrivers
    fun textAreaTypingIntoMiddle(driver: WebDriver) {
        driver.openTestPage("textAreaTypingIntoMiddle")
        driver.waitTextToBe(value = "None")

        val controlledTextInput = driver.findElement(By.id("textArea"))

        controlledTextInput.sendKeys("A")
        driver.waitTextToBe(value = "A")
        controlledTextInput.sendKeys("B")
        driver.waitTextToBe(value = "AB")
        controlledTextInput.sendKeys("C")
        driver.waitTextToBe(value = "ABC")

        controlledTextInput.sendKeys(Keys.ARROW_LEFT)
        controlledTextInput.sendKeys(Keys.ARROW_LEFT)
        assert(driver.cursorPosition("textArea") == 1)

        controlledTextInput.sendKeys("1")
        driver.waitTextToBe(value = "A1BC")

        assert(driver.cursorPosition("textArea") == 2)

        controlledTextInput.sendKeys("2")
        driver.waitTextToBe(value = "A12BC")

        assert(driver.cursorPosition("textArea") == 3)
    }
}
