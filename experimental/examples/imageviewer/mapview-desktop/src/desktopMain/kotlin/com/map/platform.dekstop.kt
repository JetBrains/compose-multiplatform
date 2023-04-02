package com.map

import java.awt.Desktop
import java.net.URL

actual fun navigateToUrl(url: String) {
    Desktop.getDesktop().browse(URL(url).toURI())
}
