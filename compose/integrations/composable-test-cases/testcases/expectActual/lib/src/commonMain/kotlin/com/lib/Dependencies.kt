package com.lib

import androidx.compose.runtime.Composable

class Abc


val Abc.commonIntVal: Int
    @Composable get() = 1000

expect val Abc.composableIntVal: Int
    @Composable get

expect fun getPlatformName(): String

@Composable
expect fun ComposableExpectActual()

@Composable
expect fun ComposableExpectActualWithDefaultParameter(p1: String = "defaultValue")