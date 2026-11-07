import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Exec

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
}

val debugKeyFile = layout.buildDirectory.file("local-signing/atlas-debug.keystore").get().asFile
val prepareDebugKey = tasks.register("prepareAtlasDebugKey") {
    outputs.file(debugKeyFile)
    doLast {
        if (!debugKeyFile.exists()) {
            debugKeyFile.parentFile.mkdirs()
            val keytool = File(System.getProperty("java.home"), "bin/keytool" + if (System.getProperty("os.name").startsWith("Windows")) ".exe" else "")
            exec {
                commandLine(keytool.absolutePath, "-genkeypair", "-noprompt", "-keystore", debugKeyFile.absolutePath,
                    "-storepass", "atlas-local", "-alias", "atlas", "-keypass", "atlas-local", "-keyalg", "RSA",
                    "-keysize", "2048", "-validity", "10000", "-dname", "CN=My Little Atlas,O=Local,C=CN")
            }
        }
    }
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    // Apple toolchains run on macOS. Windows/Android builds keep their existing task graph.
    val appleTargets = if (System.getProperty("os.name") == "Mac OS X" ||
        providers.gradleProperty("atlas.ios").orNull == "true") {
        listOf(iosArm64(), iosSimulatorArm64(), iosX64()).onEach { target ->
            target.binaries.framework {
                baseName = "AtlasShared"
                isStatic = true
                binaryOption("bundleId", "com.mylittleatlas.shared")
            }
        }
    } else emptyList()
    sourceSets {
        val jvmMain by creating { dependsOn(commonMain.get()) }
        androidMain.get().dependsOn(jvmMain)
        getByName("desktopMain").dependsOn(jvmMain)
        if (appleTargets.isNotEmpty()) {
            val iosMain by creating { dependsOn(commonMain.get()) }
            val iosTest by creating { dependsOn(commonTest.get()) }
            appleTargets.forEach { target ->
                getByName(target.name + "Main").dependsOn(iosMain)
                getByName(target.name + "Test").dependsOn(iosTest)
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.11.0")
            implementation("androidx.core:core-ktx:1.17.0")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
            }
        }
    }
}

android {
    namespace = "com.mylittleatlas.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.mylittleatlas.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    signingConfigs {
        create("atlasDebug") {
            storeFile = debugKeyFile
            storePassword = "atlas-local"
            keyAlias = "atlas"
            keyPassword = "atlas-local"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("atlasDebug") }
        getByName("release") { isMinifyEnabled = false }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("java.io.tmpdir", rootProject.layout.projectDirectory.dir("tooling/tmp").asFile.absolutePath)
}

tasks.matching { it.name == "validateSigningDebug" }.configureEach { dependsOn(prepareDebugKey) }

compose.resources {
    publicResClass = true
    packageOfResClass = "com.mylittleatlas.resources"
}

compose.desktop {
    application {
        mainClass = "com.mylittleatlas.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "MyLittleAtlas"
            packageVersion = "1.0.0"
            description = "A personal atlas of places worth going."
            vendor = "My Little Atlas"
            modules("java.sql", "jdk.unsupported")
        }
    }
}
