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

class ControlledInputsTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun textInputHardcodedValueShouldNotChange(driver: WebDriver) {
        driver.openTestPage("textInputHardcodedValueShouldNotChange")
        driver.waitTextToBe(value = "None")

        val controlledTextInput = driver.findElement(By.id("textInput"))

        controlledTextInput.sendKeys("A")
        driver.waitTextToBe(value = "hardcodedA")

        controlledTextInput.sendKeys("B")
        driver.waitTextToBe(value = "hardcodedB")

        controlledTextInput.sendKeys("C")
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

    @ResolveDrivers
    fun textAreaHardcodedValueShouldNotChange(driver: WebDriver) {
        driver.openTestPage("textAreaHardcodedValueShouldNotChange")
        driver.waitTextToBe(value = "None")

        val controlledTextArea = driver.findElement(By.id("textArea"))

        controlledTextArea.sendKeys("A")
        driver.waitTextToBe(value = "hardcodedA")

        controlledTextArea.sendKeys("B")
        driver.waitTextToBe(value = "hardcodedB")

        controlledTextArea.sendKeys("C")
        driver.waitTextToBe(value = "hardcodedC")

        check(controlledTextArea.getAttribute("value") == "hardcoded")
    }

    @ResolveDrivers
    fun textAreaMutableValueShouldGetOverridden(driver: WebDriver) {
        driver.openTestPage("textAreaMutableValueShouldGetOverridden")
        driver.waitTextToBe(value = "InitialValue")

        val controlledTextArea = driver.findElement(By.id("textArea"))
        controlledTextArea.sendKeys("ABC")

        driver.waitTextToBe(value = "OVERRIDDEN VALUE")
        check(controlledTextArea.getAttribute("value") == "OVERRIDDEN VALUE")
    }

    @ResolveDrivers
    fun textAreaMutableValueShouldChange(driver: WebDriver) {
        driver.openTestPage("textAreaMutableValueShouldChange")
        driver.waitTextToBe(value = "InitialValue")

        val controlledTextArea = driver.findElement(By.id("textArea"))

        controlledTextArea.sendKeys("A")
        driver.waitTextToBe(value = "InitialValueA")

        controlledTextArea.sendKeys("B")
        driver.waitTextToBe(value = "InitialValueAB")

        controlledTextArea.sendKeys("C")
        driver.waitTextToBe(value = "InitialValueABC")

        check(controlledTextArea.getAttribute("value") == "InitialValueABC")
    }

    @ResolveDrivers
    fun checkBoxHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("checkBoxHardcodedNeverChanges")
        driver.waitTextToBe(value = "false")

        val checkbox = driver.findElement(By.id("checkbox"))
        check(!checkbox.isSelected)

        checkbox.click()

        driver.waitTextToBe(value = "true") // input received but ignored
        check(!checkbox.isSelected)
    }

    @ResolveDrivers
    fun checkBoxMutableValueChanges(driver: WebDriver) {
        driver.openTestPage("checkBoxMutableValueChanges")
        driver.waitTextToBe(value = "false")

        val checkbox = driver.findElement(By.id("checkbox"))
        check(!checkbox.isSelected)

        checkbox.click()

        driver.waitTextToBe(value = "true")
        check(checkbox.isSelected)
    }

    @ResolveDrivers
    fun checkBoxDefaultCheckedChangesDoesntAffectState(driver: WebDriver) {
        driver.openTestPage("checkBoxDefaultCheckedChangesDoesntAffectState")
        driver.waitTextToBe(value = "true")

        val mainCheckbox = driver.findElement(By.id("checkboxMain"))
        val mirrorCheckbox = driver.findElement(By.id("checkboxMirror"))

        check(mainCheckbox.isSelected)
        check(mirrorCheckbox.isSelected)

        mirrorCheckbox.click()
        driver.waitTextToBe(value = "true")
        check(!mirrorCheckbox.isSelected)
        check(mainCheckbox.isSelected)

        mainCheckbox.click()
        driver.waitTextToBe(value = "false")
        check(!mainCheckbox.isSelected)
        check(!mirrorCheckbox.isSelected)

        mainCheckbox.click()
        driver.waitTextToBe(value = "true")
        check(mainCheckbox.isSelected)
        check(!mirrorCheckbox.isSelected)
    }

    @ResolveDrivers
    fun radioHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("radioHardcodedNeverChanges")

        val radio1 = driver.findElement(By.id("radio1"))
        val radio2 = driver.findElement(By.id("radio2"))

        check(radio1.isSelected)
        check(!radio2.isSelected)

        check(radio1.getAttribute("name") == radio2.getAttribute("name"))
        check(radio1.getAttribute("name") == "group1")
        check(radio2.getAttribute("name") == "group1")

        radio2.click()

        check(radio1.isSelected)
        check(!radio2.isSelected)
    }
    @ResolveDrivers
    fun radioMutableCheckedChanges(driver: WebDriver) {
        driver.openTestPage("radioMutableCheckedChanges")
        driver.waitTextToBe(value = "Checked - 0")

        val radio1 = driver.findElement(By.id("radio1"))
        val radio2 = driver.findElement(By.id("radio2"))

        check(!radio1.isSelected)
        check(!radio2.isSelected)

        radio2.click()
        driver.waitTextToBe(value = "Checked - 2")

        check(!radio1.isSelected)
        check(radio2.isSelected)

        radio1.click()
        driver.waitTextToBe(value = "Checked - 1")

        check(radio1.isSelected)
        check(!radio2.isSelected)
    }

    @ResolveDrivers
    fun numberHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("numberHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val numberInput = driver.findElement(By.id("numberInput"))

        check(numberInput.getAttribute("value") == "5")

        numberInput.sendKeys("1")
        driver.waitTextToBe(value = "51")

        check(numberInput.getAttribute("value") == "5")
    }

    @ResolveDrivers
    fun numberMutableChanges(driver: WebDriver) {
        driver.openTestPage("numberMutableChanges")
        driver.waitTextToBe(value = "5")

        val numberInput = driver.findElement(By.id("numberInput"))

        check(numberInput.getAttribute("value") == "5")

        numberInput.sendKeys("1")
        driver.waitTextToBe(value = "51")

        check(numberInput.getAttribute("value") == "51")
    }

    @ResolveDrivers
    fun rangeHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("rangeHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val numberInput = driver.findElement(By.id("rangeInput"))

        check(numberInput.getAttribute("value") == "21")

        numberInput.sendKeys(Keys.ARROW_RIGHT)
        driver.waitTextToBe(value = "22")
        check(numberInput.getAttribute("value") == "21")

        numberInput.sendKeys(Keys.ARROW_RIGHT)
        driver.waitTextToBe(value = "22")
        check(numberInput.getAttribute("value") == "21")
    }

    @ResolveDrivers
    fun rangeMutableChanges(driver: WebDriver) {
        driver.openTestPage("rangeMutableChanges")
        driver.waitTextToBe(value = "10")

        val numberInput = driver.findElement(By.id("rangeInput"))

        check(numberInput.getAttribute("value") == "10")

        numberInput.sendKeys(Keys.ARROW_RIGHT)
        driver.waitTextToBe(value = "11")
        check(numberInput.getAttribute("value") == "11")

        numberInput.sendKeys(Keys.ARROW_RIGHT)
        driver.waitTextToBe(value = "12")
        check(numberInput.getAttribute("value") == "12")
    }
}
