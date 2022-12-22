package org.jetbrains.compose.demo.visuals

fun main(args: Array<String>) {
    if (args.isEmpty()) return allSamples()
    when (val effect = args[0]) {
        "words" -> mainWords()
        "wave" -> mainWave(false)
        "wave-controls" -> mainWave(true)
        "NY" -> mainNY()
        else -> throw Error("Unknown effect: $effect")
    }
}
