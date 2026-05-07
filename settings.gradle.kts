enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/kpm/public/")
        maven("https://www.jetbrains.com/intellij-repository/releases/")
    }
}

plugins {
    id("com.highcapable.gropify") version "1.0.1"
}

gropify {
    global {
        jvm {
            className = rootProject.name
            includeKeys(
                "^project\\..*$".toRegex(),
                "^gradle\\..*$".toRegex(),
                "^git\\..*$".toRegex()
            )
            isRestrictedAccessEnabled = true
        }
    }

    rootProject {
        common {
            isEnabled = false
        }
    }

    projects(":core:common") {
        jvm {
            existsPropertyFiles(".gradle/git-info.properties")
            permanentKeyValues(
                "git.commit.id" to "",
                "git.commit.id.abbrev" to "",
                "git.commit.time" to ""
            )
            keyValuesRules(
                "git.commit.id.abbrev" to ValueRule(String::class)
            )
        }
    }
}

rootProject.name = "Adbrowser"

include(":app")
include(":core:common", ":core:domain", ":core:adb", ":core:logging")