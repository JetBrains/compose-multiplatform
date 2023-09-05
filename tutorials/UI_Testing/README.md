# UI Testing
The API for unit testing in Compose for Desktop is nearly identical to the [Jetpack Compose Android testing API](https://developer.android.com/jetpack/compose/testing). We highly recommended reading that first before moving on to this tutorial.

## Setting up
To start using the testing API, you will need to add the dependency on `compose.uiTestJUnit4` to your `build.gradle` file and create the directory for your tests. 

If the module is desktop-only (`kotlin("jvm")` is applied), add the dependency via:
``` kotlin
dependencies {
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(compose.desktop.currentOs)
}
```

and the directory for tests will be `src/test/kotlin`

If the module is multiplatform (`kotlin(“multiplatform”)` is applied), add it via:

``` kotlin
kotlin {
    sourceSets {
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
```

And the directory for tests will be `src/desktopTest/kotlin`

## Creating your first test
In the tests directory, create a file named `ExampleTest.kt` and paste this code into it:

```kotlin
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ExampleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun myTest(){
        rule.setContent {
            var text by remember { mutableStateOf("Hello") }
            Text(
                text = text,
                modifier = Modifier.testTag("text")
            )
            Button(
                onClick = { text = "Compose" },
                modifier = Modifier.testTag("button")
            ){
                Text("Click me")
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")
        rule.onNodeWithTag("button").performClick()
        rule.onNodeWithTag("text").assertTextEquals("Compose")
    }
}
```

Now you can run the test by either clicking ![run](https://github.com/JetBrains/compose-multiplatform/assets/5963351/2eac4041-757e-48b0-9dc2-baef82f21a7b) button in your IDE, or from the command line with
```
./gradlew test
```

