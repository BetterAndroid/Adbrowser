plugins {
    autowire(libs.plugins.kotlin.jvm)
    autowire(libs.plugins.kotlin.ksp)
    autowire(libs.plugins.jetbrains.compose)
    autowire(libs.plugins.compose.compiler)
}

group = property.project.groupName
version = property.project.app.version

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

ksp {
    arg("lyricist.generateStringsProperty", "true")
}

compose.desktop {
    application {
        mainClass = "$group.frontend.AppKt"
    }
}

dependencies {
    implementation(projects.backend)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(com.highcapable.betterandroid.compose.extension.desktop)
    implementation(cafe.adriel.lyricist.lyricist)
    ksp(cafe.adriel.lyricist.lyricist.processor)
}