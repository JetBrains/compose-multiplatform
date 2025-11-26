package org.jetbrains.compose.html2

interface AttrsScope {

    var id: String?
    fun attr(name: String, value: String)
    fun attr(name: String)
}