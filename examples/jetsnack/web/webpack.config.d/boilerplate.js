config.entry = {
    main: [require('path').resolve(__dirname, "kotlin/load.mjs")]
};

config.resolve ?? (config.resolve = {});
config.resolve.alias ?? (config.resolve.alias = {});
config.resolve.alias.skia = false;
