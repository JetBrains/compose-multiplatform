// see https://kotlinlang.org/docs/js-project-setup.html#webpack-configuration-file
// This file provides karma.config.d configuration to run tests with k/wasm and k/js.
//
// The whole body is wrapped in an IIFE so that local declarations (e.g. `path`) do not
// leak into the shared karma.conf.js scope. For k/js the Compose plugin concatenates this
// file with `compose-skiko-runtime.js`, which also declares `const path`, and top-level
// re-declarations would throw "Identifier 'path' has already been declared".
(function (config) {
    const path = require("path");

    config.browserConsoleLogOptions.level = "debug";

    const basePath = config.basePath;
    const projectPath = path.resolve(basePath, "..", "..", "..", "..");
    const generatedAssetsPath = path.resolve(projectPath, "build", "karma-webpack-out")

    const debug = message => console.log(`[karma-config] ${message}`);

    debug(`karma basePath: ${basePath}`);
    debug(`karma generatedAssetsPath: ${generatedAssetsPath}`);

    config.proxies["/"] = path.resolve(basePath, "kotlin");

    config.files = [
        {pattern: path.resolve(generatedAssetsPath, "**/*"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.png"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.cvr"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.otf"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.gif"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.ttf"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.txt"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.json"), included: false, served: true, watched: false},
        {pattern: path.resolve(basePath, "kotlin", "**/*.xml"), included: false, served: true, watched: false},
        path.resolve(basePath, "kotlin", "test_setup.js"),
    ].concat(config.files);

    function KarmaWebpackOutputFramework(config) {
        // This controller is instantiated and set during the preprocessor phase.
        const controller = config.__karmaWebpackController;

        // only if webpack has instantiated its controller
        if (!controller) {
            console.warn(
                "Webpack has not instantiated controller yet.\n" +
                "Check if you have enabled webpack preprocessor and framework before this framework"
            )
            return
        }

        config.files.push({
            pattern: `${controller.outputPath}/**/*`,
            included: false,
            served: true,
            watched: false
        })
    }

    const KarmaWebpackOutputPlugin = {
        'framework:webpack-output': ['factory', KarmaWebpackOutputFramework],
    };

    config.plugins.push(KarmaWebpackOutputPlugin);
    config.frameworks.push("webpack-output");
})(config);
