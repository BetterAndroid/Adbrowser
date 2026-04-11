plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
}

group = gropify.project.core.adb.groupName
version = gropify.project.core.adb.version

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
    implementation(projects.core.common)
    implementation(projects.core.logging)

    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.kotlin.inject.compiler.ksp)
    implementation(libs.kotlin.inject.runtime)

    testImplementation(libs.kotlin.test.junit)
}