package debugwriter

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PrintStream
import java.awt.Desktop
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.OS

fun enableDebugWritingTo(fileName: String = "output.txt") {
    System.setProperty("skiko.hardwareInfo.enabled", "true")
    val stream = PrintStream(FileOutputStream(fileName))
    System.setOut(stream)
}

fun readDebugOutput(fileName: String = "output.txt"): String {
    val inputStream: InputStream = File(fileName).inputStream()
    return inputStream.bufferedReader().use { it.readText() }
}

fun revealDebugOutput(fileName: String = "output.txt") {
    val file = File("${System.getProperty("user.dir")}/$fileName")
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().open(file)
    }
}