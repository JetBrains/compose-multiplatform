# Compose HTML string rendering

This example ports the Compose HTML landing page to common code and renders the
shared document to a string on JVM and Kotlin/JS.

It uses the local EAP string-rendering core through the included `html` build, not
the regular JS-only HTML core. Enable the EAP projects with the command-line
property below (it also applies to the included build).

From this directory, render the page with Kotlin/JS on Node.js and open it in
the default browser:

```shell
./gradlew -Pcompose.html.eap.enabled=true previewHtml
```

The task writes the generated document and its resources to
`build/preview/index.html` before opening it.

To render the complete HTML document to standard output on JVM instead:

```shell
./gradlew -Pcompose.html.eap.enabled=true renderHtml
```

To generate `build/preview/index.html` with Kotlin/JS on Node.js without opening
the browser:

```shell
./gradlew -Pcompose.html.eap.enabled=true renderHtmlJs
```
