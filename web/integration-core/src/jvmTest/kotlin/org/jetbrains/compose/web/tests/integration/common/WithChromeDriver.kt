package org.jetbrains.compose.web.tests.integration.common

import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Sleeper
import org.openqa.selenium.support.ui.WebDriverWait

interface WithChromeDriver {
    val driver: RemoteWebDriver
}

private val PATH = "http://localhost:${ServerLauncher.port}"

fun WithChromeDriver.openTestPage(test: String) {
    driver.get("$PATH?test=$test")
}

fun WithChromeDriver.waitTextToBe(textId: String = "txt", value: String) {
    WebDriverWait(
        driver, java.time.Clock.systemDefaultZone(), Sleeper.SYSTEM_SLEEPER, 1, 1
    ).until(
        ExpectedConditions.textToBe(By.id(textId), value)
    )
}
