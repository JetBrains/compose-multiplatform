plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    iosX64()
    iosArm64()
}

compose.ios {
    application {
        entryPoint = "Main_ios.kt"
        bundleID = "com.example-company.example-app"
        version = "1.2.3"

        val distributionSigning = createSigningConfiguration {
            name = "Distribution company signing"
            keychain = project.file("secrets.keychain")
            keychainPassword = System.getenv("CI_SECRET") ?: "***" // Optional, it's beter to unlock keychain
            certificateIdentity = "name_of_distribution_certificate_in_keychain"
            provisionProfile = project.file("distribution.mobileprovision")
            teamId = "TEAM_123"
        }
        val developmentSigning = createSigningConfiguration {
            name = "Development company signing"
            keychain = project.file("secrets.keychain")
            keychainPassword = System.getenv("CI_SECRET") ?: "***" // Optional, it's beter to unlock keychain
            certificateIdentity = "name_of_development_certificate_in_keychain"
            provisionProfile = project.file("development.mobileprovision")
            teamId = "TEAM_123"
        }

        deployConfigurations {
            localFile("Local") {
                //Usage: ./gradlew iosDeployLocal
                outputFile = File("~/Desktop/release-signed.ipa")
                buildConfiguration = "Release"
                signingConfiguration = developmentSigning
            }
            simulator("IPhone11_en") {
                //Usage: ./gradlew iosDeployIPhone11_en
                device = IOSDevices.IPHONE_11
                iosVersion = "15.1"
                simulatorLanguage = "en_US"
                buildConfiguration = "Debug"
                signingConfiguration = SigningConfiguration.UNSIGNED
            }
            simulator("IPad_ru") {
                //Usage: ./gradlew iosDeployIPad_ru
                device = IOSDevices.IPAD_4
                iosVersion = "15.1"
                simulatorLanguage = "ru_RU"
                buildConfiguration = "Debug"
                signingConfiguration = SigningConfiguration.UNSIGNED
            }
            connectedDevice("TrustedDevice") {
                //Usage: ./gradlew iosDeployTrustedDevice
                buildConfiguration = "Debug"
                signingConfiguration = developmentSigning
            }
        }

    }
}
