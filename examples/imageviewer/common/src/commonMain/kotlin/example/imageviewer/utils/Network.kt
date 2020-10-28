package example.imageviewer.utils

import java.net.InetAddress

fun isInternetAvailable(): Boolean {
    return try {
        val ipAddress: InetAddress = InetAddress.getByName("google.com")
        !ipAddress.equals("")
    } catch (e: Exception) {
        false
    }
}