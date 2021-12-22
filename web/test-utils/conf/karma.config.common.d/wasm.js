// see https://kotlinlang.org/docs/js-project-setup.html#webpack-configuration-file

const path = require("path");
const fs = require("fs");


config.browserConsoleLogOptions.level = "debug";

const basePath = config.basePath;
const projectPath = path.resolve(basePath, "..", "..", "..", "..", "..");
const wasmPath = path.resolve(projectPath, "build", "out", "link", "Release-wasm-wasm")

//const debug = message => console.log(`[karma-config] ${message}`);
const debug = message => fs.appendFileSync('/Users/shagen/tmp/log.log', message + "\r\n");

const a = path.resolve(".");

const projectName = path.basename(path.dirname(basePath)).replace(/-test$/, '').replace(/^web-/,'');

if (projectName == "web-core") {
    const rootBuildDir = path.resolve(__dirname, "..", "..", "..");
    const wasmPath = path.resolve(rootBuildDir, "skiko"); 

    config.proxies = { 
        "/wasm/": wasmPath
    }

    config.files = [ 
        path.resolve(wasmPath, "skiko.js"),
        {pattern: path.resolve(wasmPath, "skiko.wasm"), included: false, served: true, watched: false},
    ].concat(config.files);
}
