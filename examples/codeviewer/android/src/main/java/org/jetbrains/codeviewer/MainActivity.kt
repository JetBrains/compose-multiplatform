package org.jetbrains.codeviewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.setContent
import org.jetbrains.codeviewer.platform._HomeFolder
import org.jetbrains.codeviewer.ui.MainView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        copyAssets()
        _HomeFolder = filesDir

        setContent {
            MainView()
        }
    }

    private fun copyAssets() {
        for (filename in assets.list("data")!!) {
            assets.open("data/$filename").use { assetStream ->
                val file = File(filesDir, filename)
                FileOutputStream(file).use { fileStream ->
                    assetStream.copyTo(fileStream)
                }
            }
        }
    }
}