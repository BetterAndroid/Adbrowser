plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

group = gropify.project.core.domain.groupName
version = gropify.project.core.domain.version

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

dependencies {
    api(projects.core.common)
    api(projects.core.adb)
    api(projects.core.logging)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.kotlin.inject.compiler.ksp)
    implementation(libs.kotlin.inject.runtime)

    testImplementation(libs.kotlin.test.junit)
}