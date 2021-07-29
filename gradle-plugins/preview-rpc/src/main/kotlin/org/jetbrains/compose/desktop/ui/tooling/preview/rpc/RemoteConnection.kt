/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

fun getLocalConnectionOrNull(
    port: Int, logger: PreviewLogger, onClose: () -> Unit
): RemoteConnection? =
    getLocalSocketOrNull(port, trials = 3, trialDelay = 500)?.let { socket ->
        RemoteConnectionImpl(socket, logger, onClose)
    }

abstract class RemoteConnection : AutoCloseable {
    abstract val isAlive: Boolean
    abstract fun receiveCommand(onResult: (Command) -> Unit)
    abstract fun receiveData(onResult: (ByteArray) -> Unit)
    abstract fun sendCommand(command: Command)
    abstract fun sendData(data: ByteArray)

    fun receiveUtf8StringData(onResult: (String) -> Unit) {
        receiveData { bytes ->
            val string = bytes.toString(Charsets.UTF_8)
            onResult(string)
        }
    }
    fun sendUtf8StringData(string: String) {
        sendData(string.toByteArray(Charsets.UTF_8))
    }

    fun sendCommand(type: Command.Type, vararg args: String) {
        sendCommand(Command(type, *args))
    }
}

// Constructor is also used in GradlePluginTest#configurePreview via reflection
internal class RemoteConnectionImpl(
    private val socket: Socket,
    private val log: PreviewLogger,
    private val onClose: () -> Unit
): RemoteConnection() {
    init {
        socket.soTimeout = SOCKET_TIMEOUT_MS
    }

    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())
    private var isConnectionAlive = AtomicBoolean(true)

    override val isAlive: Boolean
        get() = !socket.isClosed && isConnectionAlive.get()

    private inline fun ifAlive(fn: () -> Unit) {
        if (isAlive) {
            fn()
        }
    }

    override fun close() {
        if (isConnectionAlive.compareAndSet(true, false)) {
            log { "CLOSING" }
            socket.close()
            onClose()
            log { "CLOSED" }
        }
    }

    override fun sendCommand(command: Command) = ifAlive {
        val commandStr = command.asString()
        val data = commandStr.toByteArray()
        writeData(output, data, maxDataSize = MAX_CMD_SIZE)
        log { "SENT COMMAND '$commandStr'" }
    }

    override fun sendData(data: ByteArray) = ifAlive {
        writeData(output, data, maxDataSize = MAX_BINARY_SIZE)
        log { "SENT DATA [${data.size}]" }
    }

    override fun receiveCommand(onResult: (Command) -> Unit) = ifAlive {
        val line = readData(input, MAX_CMD_SIZE)?.toString(Charsets.UTF_8)
        if (line != null) {
            val cmd = Command.fromString(line)
            if (cmd == null) {
                log { "GOT UNKNOWN COMMAND '$line'" }
            } else {
                log { "GOT COMMAND '$line'" }
                onResult(cmd)
            }
        } else {
            close()
        }
    }

    override fun receiveData(onResult: (ByteArray) -> Unit) = ifAlive {
        val data = readData(input, MAX_BINARY_SIZE)
        if (data != null) {
            log { "GOT [${data.size}]" }
            onResult(data)
        } else {
            close()
        }
    }

    private fun writeData(output: DataOutputStream, data: ByteArray, maxDataSize: Int): Boolean {
        if (!isAlive) return false

        return try {
            val size = data.size
            assert(size < maxDataSize) { "Data is too big: $size >= $maxDataSize" }
            output.writeInt(size)
            var index = 0
            val bufSize = minOf(MAX_BUF_SIZE, size)
            while (index < size) {
                val len = minOf(bufSize, size - index)
                output.write(data, index, len)
                index += len
            }
            output.flush()
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun readData(input: DataInputStream, maxDataSize: Int): ByteArray? {
        while (isAlive) {
            try {
                val size = input.readInt()
                if (size == -1) {
                    break
                } else {
                    assert(size < maxDataSize) { "Data is too big: $size >= $maxDataSize" }
                    val bytes = ByteArray(size)
                    val bufSize = minOf(size, MAX_BUF_SIZE)
                    var index = 0
                    while (index < size) {
                        val len = minOf(bufSize, size - index)
                        val bytesRead = input.read(bytes, index, len)
                        index += bytesRead
                    }
                    return bytes
                }
            } catch (e: IOException) {
                if (e !is SocketTimeoutException) break
            }
        }

        return null
    }
}


