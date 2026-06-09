pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

rootProject.name = "Skinwalker"
include(":app")
include(":black-reflection")
include(":compiler")
include(":Bcore")

project(":black-reflection").projectDir = file("third_party/NewBlackbox/black-reflection")
project(":compiler").projectDir = file("third_party/NewBlackbox/compiler")
project(":Bcore").projectDir = file("third_party/NewBlackbox/Bcore")
