package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

object R4ADefaultErrorMessages : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("R4A")
    override fun getMap() = MAP

    init {
        MAP.put(
            R4AErrors.DUPLICATE_ATTRIBUTE,
            "Duplicate attribute; Attributes must appear at most once per tag."
        )
        MAP.put(
            R4AErrors.OPEN_COMPONENT,
            "Component is open. Components cannot be an open or abstract class."
        )
        MAP.put(
            R4AErrors.MISMATCHED_ATTRIBUTE_TYPE,
            "Attribute {0} expects type ''{1}'', found ''{2}''",
            Renderers.STRING,
            Renderers.RENDER_TYPE,
            Renderers.RENDER_TYPE
        )
        MAP.put(
            R4AErrors.UNRESOLVED_ATTRIBUTE_KEY,
            "No valid attribute on ''{0}'' found with key ''{1}'' and type ''{2}''",
            Renderers.COMPACT,
            Renderers.STRING,
            Renderers.RENDER_TYPE
        )
        MAP.put(
            R4AErrors.MISMATCHED_ATTRIBUTE_TYPE_NO_SINGLE_PARAM_SETTER_FNS,
            "Setters with multiple arguments are currently unsupported. Found: ''{0}''",
            Renderers.COMPACT
        )
        MAP.put(
            R4AErrors.MISSING_REQUIRED_ATTRIBUTES,
            "Missing required attributes: {0}",
            Renderers.commaSeparated(Renderers.COMPACT)
        )
        MAP.put(
            R4AErrors.INVALID_TAG_TYPE,
            "Invalid KTX tag type. Found ''{0}'', Expected ''{1}''",
            Renderers.RENDER_TYPE,
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
        MAP.put(
            R4AErrors.SUSPEND_FUNCTION_USED_AS_SFC,
            "Suspend functions are not allowed to be used as R4A Components"
        )
        MAP.put(
            R4AErrors.INVALID_TYPE_SIGNATURE_SFC,
            "Only Unit-returning functions are allowed to be used as R4A Components"
        )
        MAP.put(
            R4AErrors.INVALID_TAG_DESCRIPTOR,
            "Invalid KTX tag type. Expected ''{1}",
            Renderers.commaSeparated(Renderers.RENDER_TYPE)
        )
    }
}