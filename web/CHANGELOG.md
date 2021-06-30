# Changelog

## [0.5.0-build228] - 29'June 2021

[release post](https://github.com/JetBrains/compose-jb/releases/tag/v0.5.0-build228-web)

## [0.5.0-build226] - 24'June 2021

[release post](https://github.com/JetBrains/compose-jb/releases/tag/0.5.0-build226-web)

## [0.5.0-build225] - 15'June 2021

[release post](https://github.com/JetBrains/compose-jb/releases/tag/0.5.0-build225-web)

## [0.5.0-build222] - 09'June 2021

[release post](https://github.com/JetBrains/compose-jb/releases/tag/0.5.0-build220-web)

## [0.0.0-web-dev-14] - 01'June 2021

[release post](https://github.com/JetBrains/compose-jb/releases/tag/0.0.0-web-dev-14)

## [0.0.0-web-dev-13] - 25'May 2021

### Changes in build:

**0.0.0-web-dev-13 is built against Kotlin 1.5.0**. We've also split web compose dependency into two:
one for using DOM entities and one for using Widget entities. 

We've moved all Compose for Web logic (apart from runtime) to the [compose-jb](https://github.com/JetBrains/compose-jb) repository.
At this stage of development it makes it easier to experiment, build and, most importantly to contribute to the project. 

We have artefacts for the JS target into two, the aliases which gradle compose plugin provides for this artefacts have changed accordingly.
Here's what've changed:
#### 0.0.0-web-dev-12
```kotlin
   val jsMain by getting {
            dependencies {
                implementation(compose.web.web)
                implementation(compose.runtime)
            }
        }

```

#### 0.0.0-web-dev-13
```kotlin
   val jsMain by getting {
            dependencies {
                // add this dependency if you want to use the common API widgets
                // full example here - https://github.com/JetBrains/compose-jb/tree/1f43be9c912a681a05008117574ecc1473226ffe/examples/falling_balls_with_web
                implementation(compose.web.widgets)

                // add this dependency if you want to use the DOM API
                // full example here - https://github.com/JetBrains/compose-jb/tree/1f43be9c912a681a05008117574ecc1473226ffe/examples/web_landing
                implementation(compose.web.core)
                implementation(compose.runtime)
            }
        }
```

### Changes in code:
   
In **0.0.0-web-dev-13** each `classes`  invocation will add class names instead of recreating classList completely.
To illustrate the difference, consider following code:
```kotlin
Div(attrs = {
    classes("a", "b")
    classes("c", "d")
}) {}
```
This code will create following DOM layout:

 0.0.0-web-dev-12 |  0.0.0-web-dev-13|
------------ | ------------- 
`<div class="c d">` |  `<div class="a b c d">`


Throughout the pre-alpha period in our quest for most convenient API we are not promising any version-to-version back compatibility. In this particular case, however, though this change is, strictly speaking, breaking back compatibility, it's very unlikely one was relying such behavior. Essentially, in *0.0.0-web-dev-12* it was possible to have class deletion in composition. We've failed to find any idiomatic scenarios where one will need to delete class name. Please, feel free to post an issue if you still believe that this scenario needs to be supported as well.
