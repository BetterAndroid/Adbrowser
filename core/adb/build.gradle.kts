plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
}

group = gropify.project.core.adb.groupName
version = gropify.project.core.adb.version

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.logging)

    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.kotlin.inject.compiler.ksp)
    implementation(libs.kotlin.inject.runtime)

    testImplementation(libs.kotlin.test.junit)
}