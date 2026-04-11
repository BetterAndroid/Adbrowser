import java.util.TimeZone

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.git.properties)
}

group = gropify.project.core.common.groupName
version = gropify.project.core.common.version

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    sourceSets.all { languageSettings { languageVersion = "2.0" } }
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
}

gitProperties {
    gitPropertiesName = "git-info.properties"
    gitPropertiesResourceDir = rootProject.rootDir.resolve(".gradle")
    dateFormat = "yyyy-MM-dd HH:mm:ss"
    dateFormatTimeZone = TimeZone.getDefault().id
    keys = listOf("git.commit.id", "git.commit.id.abbrev", "git.commit.time")
}

// Ensure that the git properties are generated before any task that might need them,
// such as compilation or packaging tasks.
tasks.named("generateGitProperties").get().apply {
    actions.forEach { it.execute(this) }
}

dependencies {
    implementation(libs.kotlin.inject.runtime)

    testImplementation(libs.kotlin.test.junit)
}