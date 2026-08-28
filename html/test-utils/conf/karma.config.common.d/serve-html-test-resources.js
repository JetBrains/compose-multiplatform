const path = require("path");
const fs = require("fs");
const ssrHydrationFixture = path.resolve(config.basePath, "kotlin/ssr-hydration.html");

if (fs.existsSync(ssrHydrationFixture)) {
    config.files.push({
        pattern: "kotlin/ssr-hydration.html",
        included: false,
        served: true,
        watched: false,
    });
}
