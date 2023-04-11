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

    @ResolveDrivers
    fun emailDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("emailDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "Value = None")

        val emailInput = driver.findElement(By.id("emailInput"))
        check(emailInput.getAttribute("value") == "a@a.abc")

        emailInput.clear()
        emailInput.sendKeys("u@u.com")

        driver.waitTextToBe(value = "Value = u@u.com")
        check(emailInput.getAttribute("value") == "u@u.com")

        check(driver.outerHtmlOfElementWithId("emailInput").contains("value=\"a@a.abc\""))
    }

    @ResolveDrivers
    fun passwordDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("passwordDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "Value = None")

        val passwordInput = driver.findElement(By.id("passwordInput"))
        check(passwordInput.getAttribute("value") == "1111")

        passwordInput.clear()
        passwordInput.sendKeys("a")

        driver.waitTextToBe(value = "Value = a")
        check(passwordInput.getAttribute("value") == "a")

        check(driver.outerHtmlOfElementWithId("passwordInput").contains("value=\"1111\""))
    }

    @ResolveDrivers
    fun searchDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("searchDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "Value = None")

        val searchInput = driver.findElement(By.id("searchInput"))
        check(searchInput.getAttribute("value") == "kotlin")

        searchInput.clear()
        searchInput.sendKeys("j")
        driver.waitTextToBe(value = "Value = j")

        check(searchInput.getAttribute("value") == "j")

        check(driver.outerHtmlOfElementWithId("searchInput").contains("value=\"kotlin\""))
    }

    @ResolveDrivers
    fun telDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("telDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "None")

        val telInput = driver.findElement(By.id("telInput"))
        check(telInput.getAttribute("value") == "123123")

        telInput.clear()
        telInput.sendKeys("987654321")

        driver.waitTextToBe(value = "987654321")
        check(telInput.getAttribute("value") == "987654321")
        check(driver.outerHtmlOfElementWithId("telInput").contains("value=\"123123\""))
    }

    @ResolveDrivers
    fun urlDefaultValueRemainsTheSameButValueCanBeChanged(driver: WebDriver) {
        driver.openTestPage("urlDefaultValueRemainsTheSameButValueCanBeChanged")
        driver.waitTextToBe(value = "None")

        val urlInput = driver.findElement(By.id("urlInput"))
        check(urlInput.getAttribute("value") == "www.site.com")

        urlInput.clear()
        urlInput.sendKeys("google.com")

        driver.waitTextToBe(value = "google.com")
        check(urlInput.getAttribute("value") == "google.com")
        check(driver.outerHtmlOfElementWithId("urlInput").contains("value=\"www.site.com\""))
    }
}
