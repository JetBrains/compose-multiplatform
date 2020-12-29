data class SnippetData(
  val file: File,
  val lineNumber: Int,
  val content: String,
  var tempDir: File? = null
)

fun findSnippets(dirs: List<String>): List<SnippetData> {
  val snippets = mutableListOf<SnippetData>()
  dirs.forEach { dirName ->
    println(dirName)
    rootProject
      .projectDir
      .parentFile
      .resolve(dirName)
      .listFiles()
      .filter { it.name.endsWith(".md") }
      .forEach { file ->
      val currentSnippet = kotlin.text.StringBuilder()
      var inSnippet = false
      var lineNumber = 0
      file.forEachLine { line ->
        lineNumber++
        if (line == "```kotlin")
          inSnippet = true
        else if (line == "```" && inSnippet) {
          inSnippet = false
          snippets.add(SnippetData(file, lineNumber, currentSnippet.toString()))
          currentSnippet.clear()
        } else {
          if (inSnippet) {
            currentSnippet.appendln(line)
          }
        }
      }
    }
  }
  return snippets
}

fun cloneTemplate(index: Int, content: String): File {
  val tempDir = file("${project.buildDir.absolutePath}/temp/cloned-$index")
  tempDir.deleteRecursively()
  tempDir.mkdirs()
  file("${projectDir.parentFile.parentFile.absolutePath}/templates/desktop-template").copyRecursively(tempDir)
  // tempDir.deleteOnExit()
  File("$tempDir/src/main/kotlin/main.kt").printWriter().use { out ->
    out.println(content)
  }
  return tempDir
}

fun checkDirs(dirs: List<String>) {
  val snippets = findSnippets(dirs)
  snippets.forEachIndexed { index, snippet ->
    println("process snippet $index at ${snippet.file}:${snippet.lineNumber}")
    snippet.tempDir = cloneTemplate(index, snippet.content)
    val isWin = System.getProperty("os.name").startsWith("Win")
    val procBuilder = if (isWin) {
        ProcessBuilder("gradlew.bat", "build")
    } else {
        ProcessBuilder("bash", "./gradlew", "build")
    }
    val proc = procBuilder
      .directory(snippet.tempDir)
      .redirectOutput(ProcessBuilder.Redirect.PIPE)
      .redirectError(ProcessBuilder.Redirect.PIPE)
      .start()
    proc.waitFor(5, TimeUnit.MINUTES)
    if (proc.exitValue() != 0) {
      System.err.println("Error in snippet at ${snippet.file}:${snippet.lineNumber}")
      println(proc.inputStream.bufferedReader().readText())
      println(proc.errorStream.bufferedReader().readText())
    }
  }
}

// NOTICE: currently we use a bit hacky approach, when "```kotlin" marks code that shall be checked, while "``` kotlin"
// with whitespace marks code that shall not be checked.
tasks.register("check") {
  doLast {
    val dirs = project
      .projectDir
      .parentFile
      .listFiles()
      .filter {
        it.isDirectory && it.name[0].isUpperCase() }
      .map { it.name }
    checkDirs(dirs)
  }
}
