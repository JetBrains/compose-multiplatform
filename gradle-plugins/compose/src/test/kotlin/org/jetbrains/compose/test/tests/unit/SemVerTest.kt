package org.jetbrains.compose.test.tests.unit

import org.jetbrains.compose.internal.Version
import kotlin.test.Test

class SemVerTest {
    @Test
    fun testSemVersionParser() {
        assert(Version.fromString("0") < Version.fromString("1.2.3"))
        assert(Version.fromString("2") > Version.fromString("1.2.3"))
        assert(Version.fromString("1.1") > Version.fromString("1-abc"))
        assert(Version.fromString("1.1") > Version.fromString("1"))
        assert(Version.fromString("2.0.0-RC1") > Version.fromString("2.0.0-Beta5"))
        assert(Version.fromString("2.0.0-RC2") > Version.fromString("2.0.0-RC1"))
        assert(Version.fromString("2.0.0-RC1") > Version.fromString("1.9.23"))
        assert(Version.fromString("2.0.0") > Version.fromString("2.0.0-RC1"))
        assert(Version.fromString("2.0.0-RC1") == Version.fromString("2.0.0-RC1"))
    }
}