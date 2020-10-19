package example.todo.android

import android.app.Application
import com.arkivanov.mvikotlin.timetravel.server.TimeTravelServer

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        TimeTravelServer().start()
    }
}
