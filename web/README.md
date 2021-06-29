## Prerequisites for running Selenium tests

As of now Selenium tests are turned on by default.
The minimal requirement for running this tests is to have Chrome
and chromedriver installed on your machine. Currently, we
don't install Chrome via gradle scripts, so if you are running
tests locally **make sure you have Chrome installed**.

For installing chrome driver just run following command:
```kotlin
./gradlew installWebDrivers
```
