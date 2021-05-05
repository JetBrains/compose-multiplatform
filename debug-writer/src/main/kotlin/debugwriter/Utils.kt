package debugwriter

import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PrintStream
import java.nio.file.Paths
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.OS

fun enableDebugWritingTo(fileName: String = "output.txt"): Boolean {
    System.setProperty("skiko.hardwareInfo.enabled", "true")
    try {
        val directory = File(Paths.get(fileName).getParent().toString())
        if (!directory.exists()) {
            directory.mkdir()
        }
        val stream = PrintStream(FileOutputStream("$fileName"))
        System.setOut(stream)
        return true
    } catch(e: Exception) {
        return false 
    }
}

fun readDebugOutput(fileName: String = "output.txt"): String {
    val file = File("$fileName")
    if (!file.exists()) {
        return "File $fileName does not exist."
    }
    val inputStream: InputStream = file.inputStream()
    return inputStream.bufferedReader().use { it.readText() }
}

fun revealDebugOutput(fileName: String = "output.txt"): Boolean {
    val file = File(fileName)
    if (!file.exists()) {
        return false
    }
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
        return true
    } catch(e: Exception) {
        return false
    }
}