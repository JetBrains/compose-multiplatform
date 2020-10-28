package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import org.jetbrains.codeviewer.util.TextLines
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

fun java.io.File.toProjectFile(): File = object : File {
    override val name: String get() = this@toProjectFile.name

    override val isDirectory: Boolean get() = this@toProjectFile.isDirectory

    override val children: List<File>
        get() = this@toProjectFile
            .listFiles()
            .orEmpty()
            .map { it.toProjectFile() }

    override val hasChildren: Boolean
        get() = isDirectory && listFiles()?.size ?: 0 > 0

    override suspend fun readLines(backgroundScope: CoroutineScope): TextLines {
        // linePositions can be very big, so we are using IntList instead of List<Long>
        val linePositions = IntList()
        var size by mutableStateOf(0)

        val refreshJob = backgroundScope.launch {
            delay(100)
            size = linePositions.size
            while (true) {
                delay(1000)
                size = linePositions.size
            }
        }

        backgroundScope.launch {
            readLinePositions(linePositions)
            refreshJob.cancel()
            size = linePositions.size
        }

        return object : TextLines {
            override val size get() = size

            override suspend fun get(index: Int): String {
                return withContext(Dispatchers.IO) {
                    val position = linePositions[index]
                    try {
                        RandomAccessFile(this@toProjectFile, "rws").use {
                            it.seek(position.toLong())
                            // NOTE: it isn't efficient, but simple
                            String(
                                it.readLine()
                                    .toCharArray()
                                    .map(Char::toByte)
                                    .toByteArray(),
                                Charsets.UTF_8
                            )
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        "<Error on opening the file>"
                    }
                }
            }
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun java.io.File.readLinePositions(list: IntList) = withContext(Dispatchers.IO) {
    require(length() <= Int.MAX_VALUE) {
        "Files with size over ${Int.MAX_VALUE} aren't supported"
    }

    val averageLineLength = 200
    list.clear(length().toInt() / averageLineLength)

    var isBeginOfLine = true
    var position = 0L

    try {
        FileInputStream(this@readLinePositions).use {
            val channel = it.channel
            val ib = channel.map(
                FileChannel.MapMode.READ_ONLY, 0, channel.size()
            )
            while (ib.hasRemaining()) {
                val byte = ib.get()
                if (isBeginOfLine) {
                    list.add(position.toInt())
                }
                isBeginOfLine = byte.toChar() == '\n'
                position++
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        list.clear(1)
        list.add(0)
    }

    list.compact()
}

/**
 * Compact version of List<Int> (without unboxing Int and using IntArray under the hood)
 */
private class IntList(initialCapacity: Int = 16) {
    @Volatile
    private var array = IntArray(initialCapacity)

    @Volatile
    var size: Int = 0
        private set

    fun clear(capacity: Int) {
        array = IntArray(capacity)
        size = 0
    }

    fun add(value: Int) {
        if (size == array.size) {
            doubleCapacity()
        }
        array[size++] = value
    }

    operator fun get(index: Int) = array[index]

    private fun doubleCapacity() {
        val newArray = IntArray(array.size * 2 + 1)
        System.arraycopy(array, 0, newArray, 0, size)
        array = newArray
    }

    fun compact() {
        array = array.copyOfRange(0, size)
    }
}