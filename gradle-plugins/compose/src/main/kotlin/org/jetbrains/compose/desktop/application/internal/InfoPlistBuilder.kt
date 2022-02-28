/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import java.io.File
import kotlin.reflect.KProperty

internal class InfoPlistBuilder(private val extraPlistKeysRawXml: String? = null) {
    private val values = LinkedHashMap<InfoPlistKey, String>()

    operator fun get(key: InfoPlistKey): String? = values[key]
    operator fun set(key: InfoPlistKey, value: String?) {
        if (value != null) {
            values[key] = value
        } else {
            values.remove(key)
        }
    }

    fun writeToFile(file: File) {
        file.writer().buffered().use { writer ->
            writer.run {
                appendLine("<?xml version=\"1.0\" ?>")
                appendLine("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"https://www.apple.com/DTDs/PropertyList-1.0.dtd\">")
                appendLine("<plist version=\"1.0\">")
                appendLine(" <dict>")
                for ((k, v) in values) {
                    appendLine("  <key>${k.name}</key>")
                    appendLine("  <string>$v</string>")
                }
                extraPlistKeysRawXml?.let { appendLine(it) }
                appendLine(" </dict>")
                appendLine("</plist>")
            }
        }
    }
}

internal data class InfoPlistKey(val name: String)

internal object PlistKeys {
    private operator fun getValue(thisRef: PlistKeys, property: KProperty<*>): InfoPlistKey =
        InfoPlistKey(property.name)

    val LSMinimumSystemVersion by this
    val CFBundleDevelopmentRegion by this
    val CFBundleAllowMixedLocalizations by this
    val CFBundleExecutable by this
    val CFBundleIconFile by this
    val CFBundleIdentifier by this
    val CFBundleInfoDictionaryVersion by this
    val CFBundleName by this
    val CFBundlePackageType by this
    val CFBundleShortVersionString by this
    val CFBundleSignature by this
    val LSApplicationCategoryType by this
    val CFBundleVersion by this
    val NSHumanReadableCopyright by this
    val NSSupportsAutomaticGraphicsSwitching by this
    val NSHighResolutionCapable by this
}