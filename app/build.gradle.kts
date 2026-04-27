plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = gropify.project.groupName
version = gropify.project.app.version

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
            languageSettings.optIn("androidx.compose.ui.ExperimentalComposeUiApi")
            languageSettings.optIn("androidx.compose.ui.text.ExperimentalTextApi")
            languageSettings.optIn("org.jetbrains.jewel.foundation.ExperimentalJewelApi")
        }
    }
}

ksp {
    arg("lyricist.generateStringsProperty", "true")
}

compose.desktop {
    application {
        mainClass = "$group.app.AppKt"
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
    implementation(libs.zxing.core)

    implementation(libs.betterandroid.compose.extension.desktop)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}