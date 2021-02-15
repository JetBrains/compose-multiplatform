# Signing and notarizing distributions for macOS

Apple [requires](https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution) 
all 3rd apps to be signed and notarized (checked by Apple) 
for running on recent versions of macOS. 

## What is covered
 
In this tutorial, we'll show you how to sign and notarize 
native distributions of Compose apps (in `dmg` or `pkg` formats) 
for distribution on macOS.

## Prerequisites

* [Xcode](https://developer.apple.com/xcode/). The tutorial was checked with Xcode 12.3.
* JDK 15+ (JDK 14 is not guaranteed to work). The tutorial was checked with OpenJDK 15.0.1.

## Preparing a Developer ID certificate

You will need a Developer ID certificate for signing your app.

#### Checking existing Developer ID certificates

Open https://developer.apple.com/account/resources/certificates

#### Creating a new Developer ID certificate
1. [Create a certificate signing request](https://help.apple.com/developer-account/#/devbfa00fef7):
    * Open `Keychain Access`.
    * Open the menu dialog
        ```
        Keychain Access > Certificate Assistant > Request a Certificate from a Certificate Authority
        ```
    * Enter your Developer ID email and common name.
    * Check `Save to disk` option.
2. Create and install a new certificate using your [Apple Developer account](https://developer.apple.com/account/):
    * Open https://developer.apple.com/account/resources/certificates/add
    * Choose the `Developer ID Application` certificate type.
    * Upload your Certificate Signing Request from the previous step.
    * Download and install the certificate (drag & drop the certificate into the `Keychain Access` application).

#### Viewing installed certificates

You can find all installed certificates and their keychains by running the following command:
```
/usr/bin/security find-certificate -c "Developer ID Application"
```

If you have multiple `Developer ID Application` certificates installed,
you will need to specify the path to the keychain, containing
the certificate intended for signing.

## Preparing an App ID

An App ID represents one or more applications in Apple's ecosystem.

#### Viewing existing App IDs

Open [the page](https://developer.apple.com/account/resources/identifiers/list) on Apple's developer portal.

#### Creating a new App ID


1. Open [the page](https://developer.apple.com/account/resources/identifiers/add/bundleId) on Apple's developer portal.
2. Choose `App ID` option.
3. Choose `App` type.
4. Fill the `Bundle ID` field.
    * A [bundle ID](https://developer.apple.com/documentation/bundleresources/information_property_list/cfbundleidentifier)
      uniquely identifies an application in Apple's ecosystem.
    * You can use an explicit bundle ID a wildcard, matching multiple bundle IDs.
    * It is recommended to use the reverse DNS notation (e.g.`com.yoursitename.yourappname`).
   
## Creating an app-specific password

To be able to upload an app for notarization,
you will need an app-specific password associated with your Apple ID.

Follow these steps to generate a new password:
1. Sign in to your [Apple ID](https://appleid.apple.com/account/home) account page.
2. In the Security section, click Generate Password below App-Specific Passwords.

See [this Apple support page](https://support.apple.com/en-us/HT204397) for more information
on the app-specific passwords.

## Adding an app-specific password to a keychain

To avoid remembering your one-time password, or writing it in scripts,
you can add it to the keychain by running:
```
# Any name can be used instead of NOTARIZATION_PASSWORD

xcrun altool --store-password-in-keychain-item "NOTARIZATION_PASSWORD"
             --username <apple_id>
             --password <password>
```

Then you'll be able to refer to the password like `@keychain:NOTARIZATION_PASSWORD`
without the need to write the password itself.

## Configuring Gradle

### Gradle DSL

DSL properties should be specified in `macOS` DSL block of Compose Desktop DSL:
``` kotlin
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(compose.desktop.currentOS)
}

compose.desktop {
    application {
        mainClass = "example.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)

            macOS {
                // macOS DSL settings
            }
        }
    }
}
```

### Gradle properties

Some properties can also be specified using 
[Gradle properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).

* Default Gradle properties (`compose.desktop.mac.*`) have lower priority, than DSL properties.
* Gradle properties can be specified (the items are listed in order of ascending priority):
    * In `gradle.properties` file in Gradle home;
    * In `gradle.properties` file in project's root;
    * In command-line
    ```
     ./gradlew packageDmg -Pcompose.desktop.mac.sign=true
    ```
* Note, that `local.properties` is not a standard Gradle file, so it is not supported by default.
You can load custom properties from it manually in a script, if you want.

### Configuring bundle ID

``` kotlin
macOS {
    bundleID = "com.example-company.example-app"
}
```

A [bundle ID](https://developer.apple.com/documentation/bundleresources/information_property_list/cfbundleidentifier)
uniquely identifies an application in Apple's ecosystem. 
* A bundle ID must be specified using the `bundleID` DSL property.
* Use only alphanumeric characters (`A-Z`, `a-z`, and `0-9`), hyphen (`-`) and period (`.`) characters.
* Use the reverse DNS notation of your domain (e.g.
  `com.yoursitename.yourappname`).
* The specified bundle ID must match one of your App IDs.

### Configuring signing settings

``` kotlin
macOS {
    signing { 
         sign.set(true)
         identity.set("John Doe")
         // keychain.set("/path/to/keychain") 
    }
}
```

* Set the `sign` DSL property or  to `true`.
    * Alternatively, the `compose.desktop.mac.sign` Gradle property can be used.
* Set the `identity` DSL property to the certificate's name, e.g. `"John Doe"`.
    *  Alternatively,  the `compose.desktop.mac.signing.identity` Gradle property can be  used.
* Optionally, set the `keychain` DSL property to the path to the specific keychain, containing your certificate.
    * Alternatively, the `compose.desktop.mac.signing.keychain` Gradle property can be used.
    * This step is only necessary, if multiple `Developer ID Application` certificates are installed.
  
The following Gradle properties can be used instead of DSL properties:
* `compose.desktop.mac.sign` enables or disables signing. 
  Possible values: `true` or `false`.
* `compose.desktop.mac.signing.identity` overrides the `identity` DSL property.
* `compose.desktop.mac.signing.keychain` overrides the `keychain` DSL property.

Those properties could be stored in `$HOME/.gradle/gradle.properties` to use across multiple applications.

### Configuring notarization settings

``` kotlin
macOS {
    notarization { 
         appleID.set("john.doe@example.com")
         password.set("@keychain:NOTARIZATION_PASSWORD")
    }
}
```

* Set `appleID` to your Apple ID.
  * Alternatively, the `compose.desktop.mac.notarization.appleID` can be used.
* Set `password` to the app-specific password created previously.
    * Alternatively, the `compose.desktop.mac.notarization.password` can be used.
    * Don't write raw password directly into a build script.
    * If the password was added to the keychain, as described previously, it can be referenced as
     ```
     @keychain:NOTARIZATION_PASSWORD
     ```

## Using Gradle

The following tasks are available:
* Use `createDistributable` or `packageDmg` to get a signed application
  (no separate step is required).
* Use `notarize<PACKAGING_FORMAT>` (e.g. `notarizeDmg`) to upload an application for notarization.
  Once the upload finishes, a `RequestUUID` will be printed. 
  The notarization process takes some time.
  Once the notarization process finishes, an email will be sent to you.
  Uploaded file is saved to `<BUILD_DIR>/compose/notarization/main/<UPLOAD_DATE>-<PACKAGING_FORMAT>`
* Use `checkNotarizationStatus` to check a status of 
  last notarization requests. You can also use a command-line command to check any notarization request:
```
xcrun altool --notarization-info <RequestUUID> 
             --username <Apple_ID>
             --password "@keychain:NOTARIZATION_PASSWORD"
```
