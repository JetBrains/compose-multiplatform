/* Copyright 2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license. */
package org.jetbrains.compose.desktop.application.internal

import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Merges only the resolved Skiko artifacts; Kotlin and other dependencies keep their own identity. */
internal fun mergeSkikoJars(inputs: Collection<File>, output: File, keepEntry: (String) -> Boolean = { true }) {
    val entries = sortedMapOf<String, ByteArray>()
    val services = sortedMapOf<String, MutableSet<String>>()
    for (input in inputs.sortedBy { it.name }) {
        JarFile(input).use { jar ->
            for (entry in jar.entries().asSequence().filterNot { it.isDirectory }) {
                val name = entry.name
                if (!keepEntry(name)) continue
                if (name == "META-INF/MANIFEST.MF" || name == "META-INF/INDEX.LIST" ||
                    name.matches(Regex("META-INF/[^/]+\\.(SF|RSA|DSA|EC)", RegexOption.IGNORE_CASE))) continue
                val bytes = jar.getInputStream(entry).use { it.readBytes() }
                if (name.startsWith("META-INF/services/")) {
                    services.getOrPut(name) { sortedSetOf() }.addAll(bytes.toString(Charsets.UTF_8).lineSequence()
                        .map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toList())
                } else {
                    val target = if (name.startsWith("META-INF/") &&
                        (name.substringAfterLast('/').startsWith("LICENSE") || name.substringAfterLast('/').startsWith("NOTICE")))
                        "META-INF/skiko-licenses/${input.name}/" + name.substringAfterLast('/') else name
                    val previous = entries.putIfAbsent(target, bytes)
                    check(previous == null || previous.contentEquals(bytes)) {
                        "Conflicting Skiko JAR entry '$name' in ${input.name}"
                    }
                }
            }
        }
    }
    for ((name, providers) in services) entries[name] = (providers.joinToString("\n") + "\n").toByteArray()
    entries["META-INF/MANIFEST.MF"] = "Manifest-Version: 1.0\r\n\r\n".toByteArray()
    ZipOutputStream(output.outputStream().buffered()).use { zip ->
        for ((name, bytes) in entries) {
            zip.putNextEntry(ZipEntry(name).apply { time = 0 })
            zip.write(bytes)
            zip.closeEntry()
        }
    }
}
