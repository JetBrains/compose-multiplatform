package com.example.jetsnack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.jetsnack.ui.JetsnackApp

@Composable
fun JetSnackAppEntryPoint() {
    CompositionLocalProvider(
        strsLocal provides buildStingsResources(),
        pluralsLocal provides buildPluralResources()
    ) {
        JetsnackApp()
    }
}