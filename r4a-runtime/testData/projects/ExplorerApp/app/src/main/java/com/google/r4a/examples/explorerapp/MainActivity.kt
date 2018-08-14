package com.google.r4a.examples.explorerapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        content {
            <MainComponent selected={ name ->
                val intent = Intent(this, ExampleActivity::class.java)
                intent.putExtra(EXAMPLE_NAME, name)
                startActivity(intent)
            }/>
        }
    }
}
