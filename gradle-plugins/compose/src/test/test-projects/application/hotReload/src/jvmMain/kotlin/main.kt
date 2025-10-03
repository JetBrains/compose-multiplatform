import java.io.File

fun message() = "Kotlin MPP app is running!"

fun main() {
    println(message())
    File("started").createNewFile()
    //wait for reload
    while(!message().startsWith("KMP")){
        Thread.sleep(200)
    }
    println(message())
}