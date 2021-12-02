This directory contains code that other projects in this repository expect to be able to import and reference from their build.gradle files

The files in this directory are used by the buildSrc:plugins and buildSrc:private projects.

The files in this directory are essentially a project and can be turned into a real Gradle project if needed; at the moment, they are simply included in the corresponding builds because that runs more quickly.
