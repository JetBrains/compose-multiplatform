package com.google.r4a.examples.explorerapp

import com.google.r4a.Composable
import com.google.r4a.Pivotal
import com.google.r4a.examples.explorerapp.common.data.Link
import com.google.r4a.examples.explorerapp.ui.screens.*

//@Composable
fun ExamplePage() {
    @Suppress("PLUGIN_ERROR")
    ExamplePageKt.ExamplePage()
}

//@Composable
fun ExampleList() {
    @Suppress("PLUGIN_ERROR")
    ExampleListKt.ExampleList()
}

//@Composable
fun LinkDetailScreen(@Pivotal linkId: String, pageSize: Int = 10, initialLink: Link? = null) {
    @Suppress("PLUGIN_ERROR")
    LinkDetailScreenKt.LinkDetailScreen(linkId, pageSize, initialLink)
}

//@Composable
fun LinkListScreen() {
    @Suppress("PLUGIN_ERROR")
    LinkListScreenKt.LinkListScreen()
}

//@Composable
fun LoginScreen() {
    @Suppress("PLUGIN_ERROR")
    LoginScreenKt.LoginScreen()
}

//@Composable
fun SignupScreen() {
    @Suppress("PLUGIN_ERROR")
    SignupScreenKt.SignupScreen()
}