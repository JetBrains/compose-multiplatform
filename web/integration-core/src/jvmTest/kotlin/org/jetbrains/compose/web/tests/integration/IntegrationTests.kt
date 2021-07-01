package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.Drivers
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver

class IntegrationTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun `text contains Hello World`(driver: RemoteWebDriver) {
        driver.openTestPage("helloWorldText")
        assertEquals(
            "Hello World!",
            driver.findElementByTagName("div").text
        )
    }

    @ResolveDrivers
    fun `multiple clicks on button update the counter after every click`(driver: WebDriver) {
        driver.openTestPage("buttonClicksUpdateCounterValue")

        val button = driver.findElement(By.id("btn"))

        driver.waitTextToBe(textId = "txt", value = "0")
        repeat(3) {
            button.click()
            driver.waitTextToBe(textId = "txt", value = (it + 1).toString())
        }
    }

    @ResolveDrivers
    fun `hovering the box updates the text`(driver: WebDriver) {
        driver.openTestPage("hoverOnDivUpdatesText")

        val box = driver.findElement(By.id("box"))
        driver.waitTextToBe(textId = "txt", value = "not hovered")

        val actions = Actions(driver)

        actions.moveToElement(box).perform()
        driver.waitTextToBe(textId = "txt", value = "hovered")

        actions.moveByOffset(300, 0).perform()
        driver.waitTextToBe(textId = "txt", value = "not hovered")
    }

    @ResolveDrivers
    fun `making screen width less than 400px changes the text color`(driver: WebDriver) {
        driver.openTestPage("smallWidthChangesTheTextColor")

        val initialWindowSize = driver.manage().window().size
        try {
            val span = driver.findElement(By.id("span1"))
            driver.waitTextToBe(textId = "span1", "This a colored text [expanded]")
            driver.manage().window().size = Dimension(1000, 1000)

            assertEquals("rgba(0, 200, 0, 0.92)", span.getCssValue("color"), "size 1000 x 1000")

            driver.manage().window().size = Dimension(300, 300)
            driver.waitTextToBe(textId = "span1", "This a colored text")

            assertEquals("rgba(255, 200, 0, 0.99)", span.getCssValue("color"), "size 300 x 300")
        } finally {
            driver.manage().window().size = initialWindowSize
        }
    }
}
