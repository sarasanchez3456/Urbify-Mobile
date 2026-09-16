import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Firma de release: las credenciales NUNCA viven en este archivo ni en el repo.
// Se leen de keystore.properties (local, gitignored) o, si no existe, de variables
// de entorno (pensado para CI). Ver docs/RELEASE_SIGNING.md para la guía completa.
val keystoreProperties = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        FileInputStream(propsFile).use { load(it) }
    }
}

fun releaseSigningValue(propertyKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.example.appcrud"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.appcrud"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = releaseSigningValue("storeFile", "RELEASE_STORE_FILE")
            val storePass = releaseSigningValue("storePassword", "RELEASE_STORE_PASSWORD")
            val alias = releaseSigningValue("keyAlias", "RELEASE_KEY_ALIAS")
            val keyPass = releaseSigningValue("keyPassword", "RELEASE_KEY_PASSWORD")

            if (storeFilePath != null && storePass != null && alias != null && keyPass != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = true
            }
        }
    }

    // Falla explícita y temprano si falta la firma de producción, en vez de
    // dejar que assembleRelease/bundleRelease caiga en un error críptico de
    // Gradle sobre "storeFile no configurado". Ver docs/RELEASE_SIGNING.md.
    gradle.taskGraph.whenReady {
        val runningRelease = allTasks.any {
            it.path.endsWith("assembleRelease") || it.path.endsWith("bundleRelease")
        }
        if (runningRelease && signingConfigs.getByName("release").storeFile == null) {
            throw GradleException(
                "Falta la firma de release. Configura keystore.properties (local) o las " +
                    "variables de entorno RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / " +
                    "RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD (CI). Ver docs/RELEASE_SIGNING.md."
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.location)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}