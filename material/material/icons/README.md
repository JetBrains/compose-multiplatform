# Material Iconography

## Modules / components
Material iconography is split across these modules:

 1. The `generator` module, in `generator/` - this module processes and generates Kotlin source files as part of the build step of the other modules. This module is not shipped as an artifact, and caches its outputs based on the input icons (found in `generator/raw-icons`).
 2. `material-icons-core` , in `core/` - this module contains _core_ icons, the set of most-commonly-used icons used by applications, including the icons that are required by Material components themselves, such as the menu icon. This module is fairly small and is depended on by `material`.
 3. `material-icons-extended`, in `extended/` - this module contains every icon that is not in `material-icons-core`, and has a transitive `api` dependency on `material-icons-core`, so depending on this module will provide every single Material icon (over 5000 at the time of writing). Due to the excessive size of this module, this module should ***NOT*** be included as a direct dependency of any other library, and should only be used if Proguard / R8 is enabled.
 4. `material-icons-extended-$theme`, in `extended/` - these modules each contain a specific theme from material-icons-extended, to facilitate compiling the icon soure files more quickly in parallel

## Icon Generation

Generation is split into a few distinct steps:

1. Icons are downloaded (manually) using the Google Fonts API, using the script in the `generator` module. This downloads vector drawables for every single Material icon to the `raw-icons` folder.
2. During compilation of the core and extended modules, these icons are processed to remove theme attributes that we cannot generate code for, checked to ensure that all icons exist in all themes, and then an API tracking file similar to API files in other modules is generated. This API file tracks what icons we have processed / will generate code, and the build will fail at this point if there are differences between the checked in API file and the generated API file.
3. Once these icons are processed, we then parse each file, create a Vector-like representation, and convert this to `VectorAssetBuilder` commands that during run time will create a matching source code representation of this XML file. We then write this generated Kotlin file to the output directory, where it will be compiled as part of the `core` / `extended` module's source code, as if it was manually written and checked in. Each XML file creates a corresponding Kotlin file, containing a `by lazy` property representing that icon. For more information on using the generated icons, see `androidx.compose.material.icons.Icons`.

## Adding new icons
To add new icons, simply use the icon downloading script at `generator/download_material_icons.py`, run any Gradle command that will trigger compilation of the icon modules (such as `./gradlew buildOnServer`), and follow the message in the build failure asking to confirm API changes by updating the API tracking file.

## Icon Testing
Similar to how we generate Kotlin source for each icon, we also generate a 'testing manifest' that contains a list of all the source drawables, matched to their generated code representations. This allows us to run screenshot comparison tests (`IconComparisonTest`) that compare each pixel of the generated and source drawables, to ensure we generated the correct code, and that any changes in parsing logic that causes inconsistencies with our generation logic is caught in CI.

## Useful files

 - `generator/download_material_icons.py` - script to download icons from Google Fonts API
 - `IconGenerationTask` - base Gradle task for generating icons / associated testing resources as part of the build. See subclasses for specific task logic.
 - `IconProcessor` - processes raw XML files in `generator/raw-icons`, creates a list of all icons that we will generate source for and ensures the API surface of these icons has not changed. (We do not run Metalava (or lint) on the extended module due to the extreme size of the module (5000+ source files) - running Metalava here would take hours.)
 - `IconParser` - simple XML parser that parses the processed XML files into Vector representations.
 - `VectorAssetGenerator` - converts icons parsed by `IconParser` into Kotlin source files that represent the icon.
