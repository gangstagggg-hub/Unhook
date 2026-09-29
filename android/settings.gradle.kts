pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Unhook"
include(":app")
// Hver byggefil har sitt eget filnavn. Det hindrer at app/build.gradle.kts og build.gradle.kts
// blir byttet om når mange filer lastes opp til GitHub samtidig.
project(":app").buildFileName = "app.gradle.kts"
