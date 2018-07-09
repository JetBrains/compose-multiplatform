package com.google.r4a.examples.explorerapp.common.adapters

import android.app.Application
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.content.res.Configuration
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import androidx.navigation.NavController
import com.google.r4a.Ambient

/**
 * This is  collection of Ambients that we provide and ought to be accessible by many. I think we should figure out
 * what the full set of these should be and make sure we provide some APIs that by default just provide all of them
 * from an activity or something.
 */
object Ambients {
    // NOTE(lmr): we should probably have a "support" version of all of these as well as a platform one?
    val Activity = Ambient.of<AppCompatActivity>()
    val FragmentManager = Ambient.of<FragmentManager>()
    val Fragment = Ambient.of<Fragment>()
    val Context = Ambient.of<Context>()
    val Application = Ambient.of<Application>()
    val LifecycleOwner = Ambient.of<LifecycleOwner>()
    val Configuration = Ambient.of<Configuration>()
    val NavController = Ambient.of<NavController>()
    val ViewModelProvider = Ambient.of<ViewModelProvider>()
}