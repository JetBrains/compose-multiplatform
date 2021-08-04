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

class UncontrolledInputsTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun textInputDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("textInputDefaultValueRemainsTheSameButValueCanBeChanged")

        val input = driver.findElement(By.id("textInput"))
        check(input.getAttribute("value") == "defaultInputValue")

        input.sendKeys("-TypedText")

        val inputHtml = driver.outerHtmlOfElementWithId("textInput")

        check(inputHtml.contains("value=\"defaultInputValue\""))
        check(input.getAttribute("value") == "defaultInputValue-TypedText") // this checks the `value` property of the input
        check(input.getAttribute("data-input-value") == "defaultInputValue-TypedText")
    }

    @ResolveDrivers
    fun textAreaDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("textAreaDefaultValueRemainsTheSameButValueCanBeChanged")

        val textArea = driver.findElement(By.id("textArea"))
        check(textArea.getAttribute("value") == "defaultTextAreaValue")

        textArea.sendKeys("-TypedText")

        val innerTextOfTextArea = driver.outerHtmlOfElementWithId("textArea")

        check(innerTextOfTextArea.contains(">defaultTextAreaValue</")) // inner text keeps default value
        check(textArea.getAttribute("value") == "defaultTextAreaValue-TypedText") // this checks the `value` property of the textarea
        check(textArea.getAttribute("data-text-area-value") == "defaultTextAreaValue-TypedText")
    }

    @ResolveDrivers
    fun checkBoxDefaultCheckedRemainsTheSameButCheckedCanBeChanged(driver: WebDriver) {
        driver.openTestPage("checkBoxDefaultCheckedRemainsTheSameButCheckedCanBeChanged")

        val checkbox = driver.findElement(By.id("checkbox"))

        val innerTextOfCheckbox1 = driver.outerHtmlOfElementWithId("checkbox")
        check(innerTextOfCheckbox1.contains("checked"))
        check(checkbox.getAttribute("value") == "checkbox-value")
        check(checkbox.getAttribute("data-checkbox") == "true")
        check(checkbox.isSelected)

        checkbox.click()

        val innerTextOfCheckbox2 = driver.outerHtmlOfElementWithId("checkbox")
        check(innerTextOfCheckbox2.contains("checked"))
        check(checkbox.getAttribute("value") == "checkbox-value")
        check(checkbox.getAttribute("data-checkbox") == "false")
        check(!checkbox.isSelected)
    }

    @ResolveDrivers
    fun radioDefaultCheckedRemainsTheSameButCheckedCanBeChanged(driver: WebDriver) {
        driver.openTestPage("radioDefaultCheckedRemainsTheSameButCheckedCanBeChanged")

        val radio1 = driver.findElement(By.id("radio1"))
        val radio2 = driver.findElement(By.id("radio2"))

        check(radio1.isSelected)
        check(!radio2.isSelected)

        check(driver.outerHtmlOfElementWithId("radio1").contains("checked"))
        check(!driver.outerHtmlOfElementWithId("radio2").contains("checked"))

        radio2.click()

        check(!radio1.isSelected)
        check(radio2.isSelected)

        check(driver.outerHtmlOfElementWithId("radio1").contains("checked"))
        check(!driver.outerHtmlOfElementWithId("radio2").contains("checked"))
    }

    @ResolveDrivers
    fun numberDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("numberDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "Value = None")

        val numberInput = driver.findElement(By.id("numberInput"))
        check(numberInput.getAttribute("value") == "11")

        numberInput.sendKeys("5")
        driver.waitTextToBe(value = "Value = 511")

        check(numberInput.getAttribute("value") == "511")
        check(driver.outerHtmlOfElementWithId("numberInput").contains("value=\"11\""))
    }

    @ResolveDrivers
    fun rangeDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("rangeDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "Value = None")

        val numberInput = driver.findElement(By.id("rangeInput"))
        check(numberInput.getAttribute("value") == "7")

        numberInput.sendKeys(Keys.ARROW_RIGHT)
        driver.waitTextToBe(value = "Value = 8")

        numberInput.sendKeys(Keys.ARROW_RIGHT)
        driver.waitTextToBe(value = "Value = 9")

        check(numberInput.getAttribute("value") == "9")
        check(driver.outerHtmlOfElementWithId("rangeInput").contains("value=\"7\""))
    }
}
