// Replace paths unavailable during compilation with `null`, so they will not be shown in devtools
;
(() => {
    const fs = require("fs");
    const path = require("path");

    const outDir = __dirname + "/kotlin/"
    const projecName = path.basename(__dirname);
    const mapFile = outDir + projecName + ".wasm.map"

    const sourcemap = JSON.parse(fs.readFileSync(mapFile))
    const sources = sourcemap["sources"]
    srcLoop: for (let i in sources) {
        const srcFilePath = sources[i];
        if (srcFilePath == null) continue;

        const srcFileCandidates = [
            outDir + srcFilePath,
            outDir + srcFilePath.substring("../".length),
            outDir + "../" + srcFilePath,
        ];

        for (let srcFile of srcFileCandidates) {
            if (fs.existsSync(srcFile)) continue srcLoop;
        }

        sources[i] = null;
    }

    fs.writeFileSync(mapFile, JSON.stringify(sourcemap));
})();
