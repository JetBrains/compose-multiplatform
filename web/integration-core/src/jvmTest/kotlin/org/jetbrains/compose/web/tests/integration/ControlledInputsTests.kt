/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.junit.jupiter.api.Assumptions
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver

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
    fun checkBoxMutableValueChangesThroughOnChange(driver: WebDriver) {
        driver.openTestPage("checkBoxMutableValueChangesThroughOnChange")
        driver.waitTextToBe(value = "false")

        val checkbox = driver.findElement(By.id("checkbox"))
        check(!checkbox.isSelected)

        checkbox.click()

        driver.waitTextToBe(value = "true")
        check(checkbox.isSelected)
    }

    @ResolveDrivers
    fun checkBoxMutableValueChangesForEveryOnChange(driver: WebDriver) {
        driver.openTestPage("checkBoxMutableValueChangesForEveryOnChange")
        driver.waitTextToBe(value = "true")

        val checkbox = driver.findElement(By.id("checkbox"))
        check(checkbox.isSelected)

        checkbox.click()
        driver.waitTextToBe(value = "false")
        check(!checkbox.isSelected)

        checkbox.click()
        driver.waitTextToBe(value = "true")
        check(checkbox.isSelected)

        checkbox.click()
        driver.waitTextToBe(value = "false")
        check(!checkbox.isSelected)
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
    fun radioMutableCheckedChangesThroughOnChange(driver: WebDriver) {
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

    @ResolveDrivers
    fun emailHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("emailHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val emailInput = driver.findElement(By.id("emailInput"))
        check(emailInput.getAttribute("value") == "a@a.abc")

        emailInput.sendKeys("@")
        driver.waitTextToBe(value = "a@a.abc@")

        check(emailInput.getAttribute("value") == "a@a.abc")
    }

    @ResolveDrivers
    fun emailMutableChanges(driver: WebDriver) {
        driver.openTestPage("emailMutableChanges")
        driver.waitTextToBe(value = "")

        val emailInput = driver.findElement(By.id("emailInput"))
        check(emailInput.getAttribute("value") == "")

        emailInput.sendKeys("a")
        driver.waitTextToBe(value = "a")

        check(emailInput.getAttribute("value") == "a")
    }

    @ResolveDrivers
    fun passwordHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("passwordHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val passwordInput = driver.findElement(By.id("passwordInput"))
        check(passwordInput.getAttribute("value") == "123456")

        passwordInput.sendKeys("a")
        driver.waitTextToBe(value = "123456a")

        check(passwordInput.getAttribute("value") == "123456")
    }

    @ResolveDrivers
    fun passwordMutableChanges(driver: WebDriver) {
        driver.openTestPage("passwordMutableChanges")
        driver.waitTextToBe(value = "")

        val passwordInput = driver.findElement(By.id("passwordInput"))
        check(passwordInput.getAttribute("value") == "")

        passwordInput.sendKeys("a")
        driver.waitTextToBe(value = "a")

        check(passwordInput.getAttribute("value") == "a")
    }

    @ResolveDrivers
    fun searchHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("searchHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val searchInput = driver.findElement(By.id("searchInput"))
        check(searchInput.getAttribute("value") == "hardcoded")

        searchInput.sendKeys("a")
        driver.waitTextToBe(value = "hardcodeda")

        check(searchInput.getAttribute("value") == "hardcoded")
    }

    @ResolveDrivers
    fun searchMutableChanges(driver: WebDriver) {
        driver.openTestPage("searchMutableChanges")
        driver.waitTextToBe(value = "")

        val searchInput = driver.findElement(By.id("searchInput"))
        check(searchInput.getAttribute("value") == "")

        searchInput.sendKeys("a")
        driver.waitTextToBe(value = "a")

        check(searchInput.getAttribute("value") == "a")
    }

    @ResolveDrivers
    fun telHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("telHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val telInput = driver.findElement(By.id("telInput"))
        check(telInput.getAttribute("value") == "123456")

        telInput.sendKeys("7")
        driver.waitTextToBe(value = "1234567")

        check(telInput.getAttribute("value") == "123456")
    }

    @ResolveDrivers
    fun telMutableChanges(driver: WebDriver) {
        driver.openTestPage("telMutableChanges")
        driver.waitTextToBe(value = "")

        val telInput = driver.findElement(By.id("telInput"))
        check(telInput.getAttribute("value") == "")

        telInput.sendKeys("1")
        driver.waitTextToBe(value = "1")

        check(telInput.getAttribute("value") == "1")
    }

    @ResolveDrivers
    fun urlHardcodedNeverChanges(driver: WebDriver) {
        driver.openTestPage("urlHardcodedNeverChanges")
        driver.waitTextToBe(value = "None")

        val urlInput = driver.findElement(By.id("urlInput"))
        check(urlInput.getAttribute("value") == "www.site.com")

        urlInput.sendKeys("a")
        driver.waitTextToBe(value = "www.site.coma")

        check(urlInput.getAttribute("value") == "www.site.com")
    }

    @ResolveDrivers
    fun urlMutableChanges(driver: WebDriver) {
        driver.openTestPage("urlMutableChanges")
        driver.waitTextToBe(value = "")

        val urlInput = driver.findElement(By.id("urlInput"))
        check(urlInput.getAttribute("value") == "")

        urlInput.sendKeys("w")
        driver.waitTextToBe(value = "w")

        check(urlInput.getAttribute("value") == "w")
    }

    @ResolveDrivers
    fun hardcodedDateInputNeverChanges(driver: WebDriver) {
        driver.openTestPage("hardcodedDateInputNeverChanges")
        driver.waitTextToBe(value = "None")

        val dateInput = driver.findElement(By.id("dateInput"))
        check(dateInput.getAttribute("value") == "")

        driver.sendKeysForDateInput(dateInput, 2021, 10, 22)

        driver.waitTextToBe(value = "onInput Caught")
        check(dateInput.getAttribute("value") == "")
    }

    @ResolveDrivers
    fun mutableDateInputChanges(driver: WebDriver) {
        // We skip chrome, since for some reason `sendKeys` doesn't work as expected when used for Controlled Input in Chrome
        Assumptions.assumeTrue(
            driver !is ChromeDriver,
            "chrome driver doesn't work properly when using sendKeys on Controlled Input"
        )

        driver.openTestPage("mutableDateInputChanges")
        driver.waitTextToBe(value = "")

        val dateInput = driver.findElement(By.id("dateInput"))
        check(dateInput.getAttribute("value") == "")

        driver.sendKeysForDateInput(dateInput, 2021, 10, 22)

        driver.waitTextToBe(value = "2021-10-22")
        check(dateInput.getAttribute("value") == "2021-10-22")
    }

    @ResolveDrivers
    fun hardcodedTimeNeverChanges(driver: WebDriver) {
        driver.openTestPage("hardcodedTimeNeverChanges")
        driver.waitTextToBe(value = "None")

        val timeInput = driver.findElement(By.id("time"))
        check(timeInput.getAttribute("value") == "14:00")

        timeInput.sendKeys("18:31")

        driver.waitTextToBe(value = "onInput Caught")
        check(timeInput.getAttribute("value") == "14:00")
    }

    @ResolveDrivers
    fun mutableTimeChanges(driver: WebDriver) {
        driver.openTestPage("mutableTimeChanges")
        driver.waitTextToBe(value = "")

        val timeInput = driver.findElement(By.id("time"))
        check(timeInput.getAttribute("value") == "")

        timeInput.sendKeys("18:31")

        driver.waitTextToBe(value = "18:31")
        check(timeInput.getAttribute("value") == "18:31")
    }
}
