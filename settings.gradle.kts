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
                "^gradle\\..*$".toRegex()
            )
            isRestrictedAccessEnabled = true
        }
    }

    rootProject {
        common {
            isEnabled = false
        }
    }

    projects(":shared") {
        jvm {
            val (shortCommit, commitTime) = resolveGitInfo()
            permanentKeyValues(
                "git.commit.short" to shortCommit,
                "git.commit.time" to commitTime
            )
        }
    }
}

fun resolveGitInfo(): Pair<String, String> {
    fun runGit(vararg args: String): String? {
        val process = runCatching {
            ProcessBuilder(listOf("git", *args))
                .redirectErrorStream(true)
                .start()
        }.getOrNull() ?: return null

        return runCatching {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return null
            }
            process.inputStream.bufferedReader().use { it.readText() }.trim()
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    val shortCommit = runGit("rev-parse", "--short=8", "HEAD") ?: "<Unknown>"
    val commitTime = runGit("show", "-s", "--format=%cd", "--date=format:%Y-%m-%d %H:%M:%S", "HEAD") ?: "<Unknown>"

    return shortCommit to commitTime
}

rootProject.name = "Adbrowser"

include(":frontend", ":backend", ":shared")