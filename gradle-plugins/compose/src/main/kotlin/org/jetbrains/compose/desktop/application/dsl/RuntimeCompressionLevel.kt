package org.jetbrains.compose.desktop.application.dsl

internal enum class RuntimeCompressionLevel(internal val id: Int) {
    // For ID values see the docs on "--compress" https://docs.oracle.com/javase/9/tools/jlink.htm

    NO_COMPRESSION(0),
    CONSTANT_STRING_SHARING(1),
    ZIP(2)
}