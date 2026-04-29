plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

group = gropify.project.core.domain.groupName
version = gropify.project.core.domain.version

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