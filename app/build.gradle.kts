plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = gropify.project.groupName
version = gropify.project.app.version

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
            languageSettings.optIn("androidx.compose.ui.ExperimentalComposeUiApi")
            languageSettings.optIn("androidx.compose.ui.text.ExperimentalTextApi")
            languageSettings.optIn("org.jetbrains.jewel.foundation.ExperimentalJewelApi")
        }
    }
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
}

ksp {
    arg("lyricist.generateStringsProperty", "true")
}

compose.desktop {
    application {
        mainClass = "$group.frontend.AppKt"
    }
}

dependencies {
    implementation(projects.core.domain)

    implementation(libs.jna)
    implementation(libs.jna.platform)

    ksp(libs.lyricist.processor)
    implementation(libs.lyricist)

    implementation(libs.kotlinx.coroutines.swing)

    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }
    implementation(libs.jewel.int.ui.standalone)
    implementation(libs.intellij.platform.icons)

    implementation(libs.betterandroid.compose.extension.desktop)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}