# Compose HTML string rendering

This example ports the Compose HTML landing page to common code and renders the
shared document to a string on JVM and Kotlin/JS.

From this directory, render the page with Kotlin/JS on Node.js and open it in
the default browser:

```shell
./gradlew previewHtml
```

The task writes the generated document and its resources to
`build/preview/index.html` before opening it.

To render the complete HTML document to standard output on JVM instead:

```shell
./gradlew renderHtml
```

To generate `build/preview/index.html` with Kotlin/JS on Node.js without opening
the browser:

```shell
./gradlew renderHtmlJs
```
