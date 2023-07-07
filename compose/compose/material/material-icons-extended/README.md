This project provides the Compose Material Design extended icons

To keep Kotlin compilation times down, each theme is compiled in its own Gradle project and then the resulting .class files are merged back into the output of this project

Hopefully we can revert this when parallel compilation is supported:
https://youtrack.jetbrains.com/issue/KT-46085

See https://issuetracker.google.com/issues/178207305 and https://issuetracker.google.com/issues/184959797 for more information
