enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
    id("com.highcapable.sweetproperty") version "1.0.5"
}
sweetDependency {
    isEnableVerboseMode = false
}
sweetProperty {
    global {
        sourcesCode {
            className = rootProject.name
            includeKeys(
                "^project\\..*\$".toRegex(),
                "^gradle\\..*\$".toRegex()
            )
            isEnableRestrictedAccess = true
        }
    }
    rootProject { all { isEnable = false } }
}
rootProject.name = "Adbrowser"
include(":frontend", ":backend")