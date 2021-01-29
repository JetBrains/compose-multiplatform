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

#### Viewing existing App IDs

Open https://developer.apple.com/account/resources/identifiers/list

#### Creating a new App ID

1. Open https://developer.apple.com/account/resources/identifiers/add/bundleId
2. Choose `App ID` option.
3. Choose `App` type.
4. Enter a unique id to the `Bundle ID` field. 
   It is recommended to use the reverse DNS notation for your domain (e.g.
   `com.yoursitename.yourappname`).
   
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

All properties are set in `macOS` DSL block of Compose Desktop DSL:
```kotlin
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
                bundleID = "com.example-company.example-app"
            
                signing { 
                   identity = "John Doe"
                }
                notarization {
                   appleID = "john.doe@example-company.com"
                   password = "@keychain:NOTARIZATION_PASSWORD"
                }
            }
        }
    }
}
```

1. Set `bundleID` to the unique id created previously.
2. Configure `signing` settings:
   * Set `identity` to the certificate's name, e.g.:
        * `"John Doe"`.
        * `"Developer ID Application: John Doe"`.
   * Optionally, set `keychain` value to a path to the specific keychain, containing your certificate. 
     This step is only necessary, if multiple `Developer ID Application` certificates are installed.
3. Configure `notarization` settings:
   * Set `appleID` to your Apple ID.
   * Set `password` to the app-specific password created previously.
   If the password was added to the keychain, as described previously, it can be set as
     ```
     password = "@keychain:NOTARIZATION_PASSWORD"
     ```
     
There is no separate step for the signing an application (it is signed during packaging).
You can use a project property (a environment variable) to avoid signing
if necessary.
For example, to sign only on CI, you can use something like:
```
macOS {
   if (project.findProperty("isCI") == "true") {
      signing { 
         // ... 
      }
   }
}
```

## Using Gradle

The following tasks are available:
* Use `createDistributable` or `package<PACKAGING_FORMAT>` to get a signed application
  (no separate step is required).
* Use `notarize<PACKAGING_FORMAT>` to upload an application for notarization.
  Once the upload finishes, a `RequestUUID` will be printed. 
  The notarization process takes some time.
  Once the notarization process finishes, an email will be sent to you.
* Use `checkNotarizationStatus<PACKAGING_FORMAT>` to check a status of 
  the last notarization request. You can also use a command-line command directly:
```
xcrun altool --notarization-info <RequestUUID> 
             --username <Apple_ID>
             --password "@keychain:NOTARIZATION_PASSWORD"
```
