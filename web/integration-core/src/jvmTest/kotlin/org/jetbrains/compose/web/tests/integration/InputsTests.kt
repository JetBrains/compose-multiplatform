package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.Drivers
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions

class InputsTests : BaseIntegrationTests(Drivers.Chrome) {

    @ResolveDrivers
    fun `text area input gets printed`(driver: WebDriver) {
        driver.openTestPage("textAreaInputGetsPrinted")

        val input = driver.findElement(By.id("input"))
        input.sendKeys("Hello textArea!")

        driver.waitTextToBe(textId = "txt", value = "Hello textArea!")
    }

    @ResolveDrivers
    fun `text input gets printed`(driver: WebDriver) {
        openTestPage("textInputGetsPrinted")

        val input = driver.findElement(By.id("input"))
        input.sendKeys("Hello World!")

        waitTextToBe(textId = "txt", value = "Hello World!")
    }

    @ResolveDrivers
    fun `checkbox changes the text`(driver: WebDriver) {
        openTestPage("checkBoxChangesText")

        waitTextToBe(textId = "txt", value = "not checked")

        val checkbox = driver.findElement(By.id("checkbox"))

        checkbox.click()
        waitTextToBe(textId = "txt", value = "checked")

        checkbox.click()
        waitTextToBe(textId = "txt", value = "not checked")
    }

    @ResolveDrivers
    fun `radio buttons change the text`(driver: WebDriver) {
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

    @ResolveDrivers
    fun `range updates the text`(driver: WebDriver) {
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

    @ResolveDrivers
    fun `time input updates the text`(driver: WebDriver) {
        openTestPage("timeInputChangesText")

        waitTextToBe(value = "")

        val timeInput = driver.findElement(By.id("time"))

        timeInput.sendKeys("15:00")
        waitTextToBe(value = "15:00")
    }

//    @_root_ide_package_.org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
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

//    @_root_ide_package_.org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
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

    @ResolveDrivers
    fun `file input updates the text`(driver: WebDriver) {
        openTestPage("fileInputChangesText")
        waitTextToBe(value = "")

        val fileInput = driver.findElement(By.id("file"))

        val homePath = System.getProperty("COMPOSE_WEB_INTEGRATION_TESTS_DISTRIBUTION")
        fileInput.sendKeys("$homePath/index.html")

        waitTextToBe(value = "C:\\fakepath\\index.html")
    }
}