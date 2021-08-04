This is the :buildSrc:impl project

It contains code that is used to configure other projects in this repository but that does not need to be added to the classpaths of the build scripts of those projects.

This means that if code in this project is changed, it should not necessarily modify the classpath of those projects and should not automatically invalidate the up-to-datedness of tasks applied in those projects.

See b/140265324 for more information
