package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.ComposeKotlinCompatibility
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotlinCompatibilityUnitTest {
    @Test
    fun jvm() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("1.9.21", KotlinPlatformType.jvm)
        ComposeKotlinCompatibility.checkKotlinIsSupported("1.9.22", KotlinPlatformType.jvm)
        ComposeKotlinCompatibility.checkKotlinIsSupported("2.0.0-Beta", KotlinPlatformType.jvm)
        ComposeKotlinCompatibility.checkKotlinIsSupported("2.0.0-Beta2", KotlinPlatformType.jvm)
        ComposeKotlinCompatibility.checkKotlinIsSupported("fdsfsf", KotlinPlatformType.jvm)
        ComposeKotlinCompatibility.checkKotlinIsSupported("fdsfsf", KotlinPlatformType.jvm)
    }

    @Test
    fun wasm000() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("0.0.0", KotlinPlatformType.wasm)
    }

    @Test
    fun wasmabc() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("abc", KotlinPlatformType.wasm)
    }

    @Test
    fun wasm000something() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("0.0.0-something", KotlinPlatformType.wasm)
    }

    @Test
    fun wasm0040() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("0.0.40", KotlinPlatformType.wasm)
    }

    @Test
    fun wasm000SNAPSHOT() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("0.0.0-SNAPSHOT", KotlinPlatformType.wasm)
    }

    @Test
    fun wasm1821() {
        assertThrows<IllegalStateException> {
            ComposeKotlinCompatibility.checkKotlinIsSupported("1.8.21", KotlinPlatformType.wasm)
        }
    }

    @Test
    fun wasm1822() {
        assertThrows<IllegalStateException> {
            ComposeKotlinCompatibility.checkKotlinIsSupported("1.8.22", KotlinPlatformType.wasm)
        }
    }

    @Test
    fun wasm1921() {
        assertThrows<IllegalStateException> {
            ComposeKotlinCompatibility.checkKotlinIsSupported("1.9.21", KotlinPlatformType.wasm)
        }
    }

    @Test
    fun wasm1922() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("1.9.22", KotlinPlatformType.wasm)
    }

    @Test
    fun wasm200Beta() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("2.0.0-Beta", KotlinPlatformType.wasm)
    }

    @Test
    fun wasm200Beta1() {
        ComposeKotlinCompatibility.checkKotlinIsSupported("2.0.0-Beta1", KotlinPlatformType.wasm)
    }
}