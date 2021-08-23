package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions
import java.io.File

class InputsTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun `text area input gets printed`(driver: WebDriver) {
        driver.openTestPage("textAreaInputGetsPrinted")

        val input = driver.findElement(By.id("input"))
        input.sendKeys("Hello textArea!")

        driver.waitTextToBe(textId = "txt", value = "Hello textArea!")
    }

    @ResolveDrivers
    fun `text input gets printed`(driver: WebDriver) {
        driver.openTestPage("textInputGetsPrinted")
        driver.waitTextToBe(textId = "txt", value = "Initial-")

        val input = driver.findElement(By.id("input"))
        input.sendKeys("Hello World!")

        driver.waitTextToBe(textId = "txt", value = "Initial-Hello World!")
    }

    @ResolveDrivers
    fun `checkbox changes the text`(driver: WebDriver) {
        driver.openTestPage("checkBoxChangesText")

        driver.waitTextToBe(textId = "txt", value = "not checked")

        val checkbox = driver.findElement(By.id("checkbox"))

        checkbox.click()
        driver.waitTextToBe(textId = "txt", value = "checked")

        checkbox.click()
        driver.waitTextToBe(textId = "txt", value = "not checked")
    }

    @ResolveDrivers
    fun `radio buttons change the text`(driver: WebDriver) {
        driver.openTestPage("radioButtonsChangeText")

        driver.waitTextToBe(textId = "txt", value = "-")

        val r1 = driver.findElement(By.id("r1"))
        val r2 = driver.findElement(By.id("r2"))

        r1.click()
        driver.waitTextToBe(textId = "txt", "r1")

        r2.click()
        driver.waitTextToBe(textId = "txt", "r2")

        r1.click()
        driver.waitTextToBe(textId = "txt", "r1")

        r2.click()
        driver.waitTextToBe(textId = "txt", "r2")
    }

    @ResolveDrivers
    fun `range updates the text`(driver: WebDriver) {
        driver.openTestPage("rangeInputChangesText")
        driver.waitTextToBe(value = "0")

        val slider = driver.findElement(By.id("slider"))

        val actions = Actions(driver)

        actions
            .clickAndHold(slider)
            .sendKeys(Keys.RIGHT)
            .perform()

        driver.waitTextToBe(value = "55")
    }

    @ResolveDrivers
    fun `time input updates the text`(driver: WebDriver) {
        driver.openTestPage("timeInputChangesText")

        driver.waitTextToBe(value = "")

        val timeInput = driver.findElement(By.id("time"))

        timeInput.sendKeys("15:00")
        driver.waitTextToBe(value = "15:00")
    }

    @ResolveDrivers
    fun `date input updates the text`(driver: WebDriver) {
        driver.openTestPage("dateInputChangesText")

        driver.waitTextToBe(value = "")

        val dateInput = driver.findElement(By.id("date"))

        // we use the same value of month and day here to avoid a need for a more complex formatting
        driver.sendKeysForDateInput(dateInput, 2021, 10, 10)

        driver.waitTextToBe(value = "2021-10-10")
    }

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
        driver.openTestPage("fileInputChangesText")
        driver.waitTextToBe(value = "")

        val fileInput = driver.findElement(By.id("file"))

        val homePath = System.getProperty("COMPOSE_WEB_INTEGRATION_TESTS_DISTRIBUTION")
        fileInput.sendKeys(
            File(homePath).resolve("index.html").absolutePath
        )

        driver.waitTextToBe(value = "C:\\fakepath\\index.html")
    }

    @ResolveDrivers
    fun `onInvalid updates the text`(driver: WebDriver) {
        driver.openTestPage("invalidInputUpdatesText")
        driver.waitTextToBe(value  = "None")

        val input = driver.findElement(By.id("numberInput"))
        val submitBtn = driver.findElement(By.id("submitBtn"))

        input.sendKeys("1000")
        driver.waitTextToBe(value = "SOMETHING ENTERED")

        submitBtn.click()
        driver.waitTextToBe(value = "INVALID VALUE ENTERED")
    }

    @ResolveDrivers
    fun `onChange updates the text`(driver: WebDriver) {
        driver.openTestPage("changeEventUpdatesText")
        driver.waitTextToBe(value = "None")

        val input = driver.findElement(By.id("numberInput"))
        input.sendKeys("1")

        driver.waitTextToBe(value = "None")
        driver.findElement(By.id("txt")).click() // to change the focus - triggers onChange

        driver.waitTextToBe(value = "1")
    }

    @ResolveDrivers
    fun `onChange in TextArea updates the text`(driver: WebDriver) {
        driver.openTestPage("changeEventInTextAreaUpdatesText")
        driver.waitTextToBe(value = "None")

        val textArea = driver.findElement(By.id("textArea"))
        textArea.sendKeys("NewText")

        driver.waitTextToBe(value = "None")
        driver.findElement(By.id("txt")).click() // to change the focus - triggers onChange

        driver.waitTextToBe(value = "NoneNewText")
    }

    @ResolveDrivers
    fun `onBeforeInput updates the text`(driver: WebDriver) {
        driver.openTestPage("beforeInputEventUpdatesText")
        driver.waitTextToBe(value = "None")

        val input = driver.findElement(By.id("textInput"))

        input.sendKeys("1")
        driver.waitTextToBe(value = "1")
        driver.waitTextToBe(value = "1", textId = "txt2")

        input.sendKeys("2")
        driver.waitTextToBe(value = "2")
        driver.waitTextToBe(value = "12", textId = "txt2")
    }

    @ResolveDrivers
    fun `onBeforeInput in TextArea updates the text`(driver: WebDriver) {
        driver.openTestPage("beforeInputEventInTextAreaUpdatesText")
        driver.waitTextToBe(value = "None")

        val textArea = driver.findElement(By.id("textArea"))

        textArea.sendKeys("1")
        driver.waitTextToBe(value = "1")
        driver.waitTextToBe(value = "1", textId = "txt2")

        textArea.sendKeys("2")
        driver.waitTextToBe(value = "2")
        driver.waitTextToBe(value = "12", textId = "txt2")
    }
}
