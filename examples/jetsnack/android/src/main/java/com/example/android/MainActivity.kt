package com.example.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.jetsnack.R
import com.example.jetsnack.ui.JetsnackApp
import com.example.jetsnack.ui.theme.Karla
import com.example.jetsnack.ui.theme.Montserrat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Montserrat = FontFamily(
            Font(R.font.montserrat_light, FontWeight.Light),
            Font(R.font.montserrat_regular, FontWeight.Normal),
            Font(R.font.montserrat_medium, FontWeight.Medium),
            Font(R.font.montserrat_semibold, FontWeight.SemiBold)
        )
        Karla = FontFamily(
            Font(R.font.karla_regular, FontWeight.Normal),
            Font(R.font.karla_bold, FontWeight.Bold)
        )

        setContent {
            MaterialTheme {
                JetsnackApp()
            }
        }
    }
}