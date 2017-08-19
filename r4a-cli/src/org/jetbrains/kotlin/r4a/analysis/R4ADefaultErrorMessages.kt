package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap

object R4ADefaultErrorMessages : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("R4A")
    override fun getMap() = MAP

    init {
        MAP.put(
                R4AErrors.DUPLICATE_ATTRIBUTE,
                "Duplicate attribute; Attributes must appear at most once per tag.")
    }
}