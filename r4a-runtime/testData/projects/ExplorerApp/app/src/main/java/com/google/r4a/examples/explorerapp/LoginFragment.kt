package com.google.r4a.examples.explorerapp

import android.widget.TextView
import com.google.r4a.*
import com.google.r4a.examples.explorerapp.common.adapters.*
import com.google.r4a.examples.explorerapp.ui.screens.LoginScreen

class LoginFragment: ComposeFragment() {
    override fun compose() {
        with(composer) {
            call(
                    0,
                    { LoginScreen() },
                    { true },
                    { f -> f() }
            )
        }
    }
}