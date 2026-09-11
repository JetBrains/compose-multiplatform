// The eap/core Kotlin/JS tests copy their generated SSR fixtures to Karma's working directory.
config.files.push({
    pattern: "*ssr*hydration*.html",
    included: false,
    served: true,
    watched: false,
});

for (const fixture of [
    "ssr-hydration.html",
    "ssr-hydration-state.html",
    "ssr-number-hydration.html",
]) {
    config.proxies[`/${fixture}`] = `/base/${fixture}`;
}
