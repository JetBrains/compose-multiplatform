This example project can be built and run for desktop, web and macos. 

- To build this project for all 3 targets, one need to have compose-mpp published to their mavenLocal:
https://github.com/JetBrains/compose-jb/blob/master/compose/README.md#multiplatform-build

- Using compose version 1.0.1 (already published and available), this project can be built for js and jvm. 


**Available experiments:**
```kotlin
var numberOfItems = 1000
// Experiment1.kt
addNComposableItems(numberOfItems)

// Experiment2.kt
removeAllItems(preAddedItemsCount = numberOfItems)

// Experiment3.kt
updateEveryXth(numberOfItems, xth = 2, repeatUpdate = 20)
```

The experiments can be run as tests in `commonTest`:
`./gradlew jsTest jvmTest macosX64Test` or `macosArm64Test`

It should print results into output.

**MacOs:**

For MacOs it makes sense to run it as an app in production mode:
`./gradlew runReleaseExecutableMacosX64` or `./gradlew runReleaseExecutableArm64`
In production, it shows much better results.

See `macosMain/Main.kt` to choose what experiment to run.

**Browser (if running the app (jsMain/Main.kt) in a browser)**

We have modified `webpack.config.d` to make the app run better in development mode:
```javascript
config.devtool = undefined;  // default is `eval-source-map`
config.mode = 'none'; // default is `development`
```
This prevents kotlin source maps being added into the bundle.
The default config options add a huge overhead and app's performance is much worse.
