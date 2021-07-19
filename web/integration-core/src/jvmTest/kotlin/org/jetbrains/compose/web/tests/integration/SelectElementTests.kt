package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.Select

class SelectElementTests : BaseIntegrationTests() {

    // @ResolveDrivers // TODO: investigate. The test is flaky
    fun selectDispatchesInputAndChangeAndBeforeInputEvents(driver: WebDriver) {
        driver.openTestPage("selectDispatchesInputAndChangeAndBeforeInputEvents")
        driver.waitTextToBe(textId = "txt_oninput", value = "None")
        driver.waitTextToBe(textId = "txt_onchange", value = "None")

        // Commented code does force onChange, but doesn't force onInput for some reason
        // val select = Select(driver.findElement(By.name("pets")))
        // select.selectByIndex(0)

        // Code below works properly: forces both onInput and onChange
        driver.findElement(By.name("pets")).sendKeys("Dog")

        driver.waitTextToBe(textId = "txt_onchange", value = "dog")
        driver.waitTextToBe(textId = "txt_oninput", value = "dog")
    }

     // @ResolveDrivers // TODO: investigate. The test is flaky
    fun selectMultipleItems(driver: WebDriver) {
        driver.openTestPage("selectMultipleItems")
        driver.waitTextToBe(value = "None")

        val select = Select(driver.findElement(By.name("pets")))
        select.selectByIndex(1)

        driver.waitTextToBe(value = "dog")

        select.selectByIndex(2)
        driver.waitTextToBe(value = "dog, cat")

        select.selectByIndex(3)
        driver.waitTextToBe(value = "dog, cat, hamster")

        select.deselectByIndex(2)
        driver.waitTextToBe(value = "dog, hamster")
    }
}
