package com.example.common

enum class Platform {
    Desktop,
    Native,
    Js,
    Wasm
}

expect fun currentPlatform(): Platform