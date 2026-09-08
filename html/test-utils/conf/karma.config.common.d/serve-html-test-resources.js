// Each module's generated SSR hydration fixtures are copied into its Kotlin test resources.
config.files.push({
    pattern: "kotlin/*ssr*hydration*.html",
    included: false,
    served: true,
    watched: false,
});
