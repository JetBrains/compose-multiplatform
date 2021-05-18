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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
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
}
