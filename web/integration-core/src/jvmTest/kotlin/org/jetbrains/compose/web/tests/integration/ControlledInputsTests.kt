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
import org.openqa.selenium.WebDriver

class ControlledInputsTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun textInputHardcodedValueShouldNotChange(driver: WebDriver) {
        driver.openTestPage("textInputHardcodedValueShouldNotChange")
        driver.waitTextToBe(value = "None")

        val controlledTextInput = driver.findElement(By.id("textInput"))
        controlledTextInput.sendKeys("ABC")

        driver.waitTextToBe(value = "hardcodedC")
        check(controlledTextInput.getAttribute("value") == "hardcoded")
    }

    @ResolveDrivers
    fun textInputMutableValueShouldGetOverridden(driver: WebDriver) {
        driver.openTestPage("textInputMutableValueShouldGetOverridden")
        driver.waitTextToBe(value = "InitialValue")

        val controlledTextInput = driver.findElement(By.id("textInput"))
        controlledTextInput.sendKeys("ABC")

        driver.waitTextToBe(value = "OVERRIDDEN VALUE")
        check(controlledTextInput.getAttribute("value") == "OVERRIDDEN VALUE")
    }

    @ResolveDrivers
    fun textInputMutableValueShouldChange(driver: WebDriver) {
        driver.openTestPage("textInputMutableValueShouldChange")
        driver.waitTextToBe(value = "InitialValue")

        val controlledTextInput = driver.findElement(By.id("textInput"))

        controlledTextInput.sendKeys("A")
        driver.waitTextToBe(value = "InitialValueA")

        controlledTextInput.sendKeys("B")
        driver.waitTextToBe(value = "InitialValueAB")

        controlledTextInput.sendKeys("C")
        driver.waitTextToBe(value = "InitialValueABC")

        check(controlledTextInput.getAttribute("value") == "InitialValueABC")
    }
}
