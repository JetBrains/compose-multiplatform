/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.interactions.Actions

class InputsTests : BaseIntegrationTests() {

    @Test
    fun `text area input gets printed`() {
        openTestPage("textAreaInputGetsPrinted")

        val input = driver.findElement(By.id("input"))
        input.sendKeys("Hello textArea!")

        waitTextToBe(textId = "txt", value = "Hello textArea!")
    }

    @Test
    fun `text input gets printed`() {
        openTestPage("textInputGetsPrinted")

        val input = driver.findElement(By.id("input"))
        input.sendKeys("Hello World!")

        waitTextToBe(textId = "txt", value = "Hello World!")
    }

    @Test
    fun `checkbox changes the text`() {
        openTestPage("checkBoxChangesText")

        waitTextToBe(textId = "txt", value = "not checked")

        val checkbox = driver.findElement(By.id("checkbox"))

        checkbox.click()
        waitTextToBe(textId = "txt", value = "checked")

        checkbox.click()
        waitTextToBe(textId = "txt", value = "not checked")
    }

    @Test
    fun `radio buttons change the text`() {
        openTestPage("radioButtonsChangeText")

        waitTextToBe(textId = "txt", value = "-")

        val r1 = driver.findElement(By.id("r1"))
        val r2 = driver.findElement(By.id("r2"))

        r1.click()
        waitTextToBe(textId = "txt", "r1")

        r2.click()
        waitTextToBe(textId = "txt", "r2")

        r1.click()
        waitTextToBe(textId = "txt", "r1")

        r2.click()
        waitTextToBe(textId = "txt", "r2")
    }

    @Test
    fun `range updates the text`() {
        openTestPage("rangeInputChangesText")
        waitTextToBe(value = "0")

        val slider = driver.findElement(By.id("slider"))

        val actions = Actions(driver)

        actions.moveToElement(slider)
            .moveByOffset(-(slider.size.width / 2), 0)
            .click()
            .sendKeys(Keys.RIGHT, Keys.RIGHT)
            .perform()

        waitTextToBe(value = "10")
    }

    @Test
    fun `time input updates the text`() {
        openTestPage("timeInputChangesText")

        waitTextToBe(value = "")

        val timeInput = driver.findElement(By.id("time"))

        timeInput.sendKeys("15:00")
        waitTextToBe(value = "15:00")
    }

//    @Test
//    fun `date input updates the text`() {
//        openTestPage("dateInputChangesText")
//
//        waitTextToBe(value = "")
//
//        val timeInput = driver.findElement(By.id("date"))
//
//        timeInput.sendKeys("12102021")
//        waitTextToBe(value = "2021-10-12")
//    }

//    @Test
//    fun `dateTimeLocal input updates the text`() { // WARNING: It's not supported in Firefox
//        openTestPage("dateTimeLocalInputChangesText")
//
//        waitTextToBe(value = "")
//
//        val timeInput = driver.findElement(By.id("dateTimeLocal"))
//
//        timeInput.sendKeys("12102021", Keys.TAB, "0925AM")
//        waitTextToBe(value = "2021-10-12T09:25")
//    }

    @Test
    fun `file input updates the text`() {
        openTestPage("fileInputChangesText")
        waitTextToBe(value = "")

        val fileInput = driver.findElement(By.id("file"))

        val homePath = System.getProperty("COMPOSE_WEB_INTEGRATION_TESTS_DISTRIBUTION")
        fileInput.sendKeys("$homePath/index.html")

        waitTextToBe(value = "C:\\fakepath\\index.html")
    }
}