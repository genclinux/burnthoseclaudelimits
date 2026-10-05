// Standalone desktop build that renders the app's wallpaper designs to PNG.
// It compiles the platform-neutral `art` package straight from the app sources,
// so it needs no Android SDK:  gradle -p tools/preview run
pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "noor-preview"
