package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver

@ExtendWith(value = [StaticServerSetupExtension::class])
abstract class BaseIntegrationTests {

    companion object : WithChromeDriver {
        override val driver: RemoteWebDriver = ChromeDriver(
            ChromeOptions().apply {
                setHeadless(true)
                addArguments("--no-sandbox")
            }
        )
    }
}