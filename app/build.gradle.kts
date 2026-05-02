import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

group = gropify.project.groupName
version = gropify.project.app.version

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
        resolveJbrHomeOrNull()?.also { javaHome = it }

        nativeDistributions {
            packageName = gropify.project.name
            packageVersion = gropify.project.version.toNativePackageVersion()
            description = gropify.project.description
            copyright = gropify.project.copyright

            appResourcesRootDir.set(layout.projectDirectory.dir("packaging/resources"))
            modules("jdk.localedata")

            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )
        }
    }
}

val macOSBundleLocalizationResources = layout.projectDirectory.dir("packaging/macos")

val injectMacOSBundleLocalizations = tasks.register<InjectMacOSBundleLocalizationsTask>(
    "injectMacOSBundleLocalizations"
) {
    enabled = System.getProperty("os.name").contains("mac", ignoreCase = true)
    resourceRootDir.set(macOSBundleLocalizationResources)
    appBundleDir.set(
        layout.buildDirectory.dir(
            "compose/binaries/main/app/${gropify.project.name}.app"
        )
    )
}

val injectMacOSBundleLocalizationsRelease = tasks.register<InjectMacOSBundleLocalizationsTask>(
    "injectMacOSBundleLocalizationsRelease"
) {
    enabled = System.getProperty("os.name").contains("mac", ignoreCase = true)
    resourceRootDir.set(macOSBundleLocalizationResources)
    appBundleDir.set(
        layout.buildDirectory.dir(
            "compose/binaries/main-release/app/${gropify.project.name}.app"
        )
    )
}

tasks.matching {
    it.name == "createDistributable"
}.configureEach {
    finalizedBy(injectMacOSBundleLocalizations)
}

injectMacOSBundleLocalizations.configure {
    dependsOn(tasks.matching { it.name == "createDistributable" })
}

tasks.matching {
    it.name == "createReleaseDistributable"
}.configureEach {
    finalizedBy(injectMacOSBundleLocalizationsRelease)
}

injectMacOSBundleLocalizationsRelease.configure {
    dependsOn(tasks.matching { it.name == "createReleaseDistributable" })
}

tasks.matching {
    it.name == "packageDmg" || it.name == "packageDistributionForCurrentOS"
}.configureEach {
    dependsOn(injectMacOSBundleLocalizations)
}

tasks.matching {
    it.name == "packageReleaseDmg" || it.name == "packageReleaseDistributionForCurrentOS"
}.configureEach {
    dependsOn(injectMacOSBundleLocalizationsRelease)
}

/**
 * Compose packaging uses the JDK behind `jpackage` rather than implicitly switching to JBR.
 *
 * This project relies on JBR APIs at runtime, so packaging resolves the JBR JDK from environment
 * variables first.
 *
 * Resolution order:
 * 1. `ADBROWSER_JBR_HOME`
 * 2. `JBR_HOME`
 * 3. `JAVA_HOME`
 *
 * When neither variable is present we intentionally do nothing here and let Compose Desktop's own
 * runtime checks decide whether the current toolchain is suitable for native packaging.
 */
fun resolveJbrHomeOrNull() =
    System.getenv("ADBROWSER_JBR_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv("JBR_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv("JAVA_HOME")?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Native installers have stricter version rules than the app's own display version.
 *
 * Compose forwards this version into jpackage metadata, and formats such as macOS DMG reject a
 * leading zero major component. We keep the project version untouched and only coerce the package
 * version to a legal native-distribution value here.
 */
fun String.toNativePackageVersion(): String {
    val normalized = trim().removeSurrounding("\"")
    if (normalized.startsWith("0.")) return "1.0.0"

    return normalized
}

/**
 * Compose Desktop currently exposes app-level resources cleanly, but macOS bundle-localized
 * resources still need to live under the generated `.app/Contents/Resources`.
 *
 * We keep this injection in the packaging phase so native AWT/macOS UI can resolve bundle
 * localizations without patching built artifacts manually afterward.
 */
abstract class InjectMacOSBundleLocalizationsTask : DefaultTask() {

    @get:InputDirectory
    abstract val resourceRootDir: DirectoryProperty

    @get:OutputDirectory
    abstract val appBundleDir: DirectoryProperty

    @TaskAction
    fun inject() {
        val sourceRoot = resourceRootDir.asFile.get()
        if (!sourceRoot.exists()) return

        val bundleDir = appBundleDir.asFile.get()
        val targetResourcesDir = bundleDir.resolve("Contents/Resources")
        if (!targetResourcesDir.exists()) return

        sourceRoot.listFiles()?.forEach { child ->
            val target = targetResourcesDir.resolve(child.name)
            if (child.isDirectory) {
                child.copyRecursively(target, overwrite = true)
            } else {
                child.copyTo(target, overwrite = true)
            }
        }
    }
}

dependencies {
    implementation(projects.core.domain)

    implementation(libs.jna)
    implementation(libs.jna.platform)

    ksp(libs.lyricist.processor)
    implementation(libs.lyricist)

    implementation(libs.kotlinx.coroutines.swing)

    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)

    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }

    compileOnly(libs.jbr.api)
    implementation(libs.jewel.int.ui.standalone)
    implementation(libs.intellij.platform.icons)
    implementation(libs.flatlaf)

    implementation(libs.zxing.core)
    implementation(libs.betterandroid.compose.extension.desktop)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}