fun main(args: Array<String>) {
    println("Running test options with spaces!")
    for ((i, arg) in args.withIndex()) {
        println("Arg #${i + 1}=$arg")
    }
    println("JVM system property arg=${java.lang.System.getProperty("compose.test.arg.value")}")
}