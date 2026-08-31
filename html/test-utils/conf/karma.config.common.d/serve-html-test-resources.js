const path = require("path");
const fs = require("fs");
const ssrHydrationFixtures = [
    "ssr-hydration.html",
    "ssr-number-hydration.html",
    "ssr-hydration-data.html",
];

ssrHydrationFixtures.forEach((fixture) => {
    if (fs.existsSync(path.resolve(config.basePath, "kotlin", fixture))) {
        config.files.push({
            pattern: "kotlin/" + fixture,
            included: false,
            served: true,
            watched: false,
        });
    }
});
