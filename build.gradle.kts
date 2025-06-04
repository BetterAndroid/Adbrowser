plugins {
    autowire(libs.plugins.kotlin.jvm) apply false
    autowire(libs.plugins.kotlin.ksp) apply false
    autowire(libs.plugins.jetbrains.compose) apply false
    autowire(libs.plugins.compose.compiler) apply false
}