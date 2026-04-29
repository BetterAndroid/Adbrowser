plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
}

group = gropify.project.core.logging.groupName
version = gropify.project.core.logging.version

dependencies {
    implementation(projects.core.common)

    ksp(libs.kotlin.inject.compiler.ksp)
    implementation(libs.kotlin.inject.runtime)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j2.impl)

    testImplementation(libs.kotlin.test.junit)
}