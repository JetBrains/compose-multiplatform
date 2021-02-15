package org.jetbrains.compose.desktop.application.internal

import java.io.File
import java.util.*

internal const val NOTARIZATION_REQUEST_INFO_FILE_NAME = "notarization-request.properties"

internal data class NotarizationRequestInfo(
    var uuid: String = "",
    var uploadTime: String = ""
) {
    fun loadFrom(file: File) {
        val properties = Properties().apply {
            file.inputStream().buffered().use { input ->
                load(input)
            }
        }
        uuid = properties.getProperty(UUID) ?: uuid
        uploadTime = properties.getProperty(UPLOAD_TIME) ?: uploadTime
    }

    fun saveTo(file: File) {
        val properties = Properties()
        properties[UUID] = uuid
        properties[UPLOAD_TIME] = uploadTime
        file.outputStream().buffered().use { output ->
            properties.store(output, null)
        }
    }

    companion object {
        private const val UUID = "uuid"
        private const val UPLOAD_TIME = "upload.time"
    }
}