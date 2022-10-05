### Use Compose(web) in React app

[see ComposeInReactApp.kt](src/jsMain/kotlin/ComposeInReactApp.kt)

`useCompose(...)` is a custom React effect to render a content using Compose.
It's not a part of any library.

### Use React in Compose(web) app

`UseReactEffect(...)` is a custom Compose effect to render a content using React.
It's not a part of any library.

[see ReactInComposeApp.kt](src/jsMain/kotlin/ReactInComposeApp.kt)

### How to use existing React components:

It requires adding `external` declarations. For example: [ReactYoutubePlayer.kt](src/jsMain/kotlin/ReactYoutubePlayer.kt)

Here is a good tutorial - [Using packages from NPM](https://play.kotlinlang.org/hands-on/Building%20Web%20Applications%20with%20React%20and%20Kotlin%20JS/07_Using_Packages_From_NPM)

### Running web application
* To run, launch command: `./gradlew :jsBrowserRun`
* Or choose **browser** configuration in IDE and run it.  
  ![browser-run-configuration.png](screenshots/browser-run-configuration.png)
