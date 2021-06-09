package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.interactions.Actions

class IntegrationTests : BaseIntegrationTests() {

    @Test
    fun `text contains Hello World`() {
        openTestPage("helloWorldText")
        assertEquals(
            "Hello World!",
            driver.findElementByTagName("div").text
        )
    }

    @Test
    fun `multiple clicks on button update the counter after every click`() {
        openTestPage("buttonClicksUpdateCounterValue")

        val button = driver.findElement(By.id("btn"))

        waitTextToBe(textId = "txt", value = "0")
        repeat(3) {
            button.click()
            waitTextToBe(textId = "txt", value = (it + 1).toString())
        }
    }

    @Test
    fun `hovering the box updates the text`() {
        openTestPage("hoverOnDivUpdatesText")

        val box = driver.findElement(By.id("box"))
        waitTextToBe(textId = "txt", value = "not hovered")

        val actions = Actions(driver)

        actions.moveToElement(box).perform()
        waitTextToBe(textId = "txt", value = "hovered")

        actions.moveByOffset(300, 0).perform()
        waitTextToBe(textId = "txt", value = "not hovered")
    }

    @Test
    fun `making screen width less than 400px changes the text color`() {
        openTestPage("smallWidthChangesTheTextColor")

        val initialWindowSize = driver.manage().window().size
        try {
            val span = driver.findElement(By.id("span1"))
            waitTextToBe(textId = "span1", "This a colored text")
            driver.manage().window().size = Dimension(1000, 1000)

            assertEquals("rgba(0, 0, 0, 1)", span.getCssValue("color"))

            driver.manage().window().size = Dimension(300, 300)
            waitTextToBe(textId = "span1", "This a colored text")

            assertEquals("rgba(255, 0, 0, 1)", span.getCssValue("color"))
        } finally {
            driver.manage().window().size = initialWindowSize
        }
    }
}
