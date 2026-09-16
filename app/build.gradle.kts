plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * URLs configurables mediante -P<ENTORNO>_API_BASE_URL, gradle.properties o
 * local.properties. Retrofit exige que la URL base termine en '/'.
 */
fun apiBaseUrl(propertyName: String, defaultValue: String, requireHttps: Boolean = false): String {
    val value = providers.gradleProperty(propertyName).orElse(defaultValue).get()
    require(value.endsWith('/')) { "$propertyName debe terminar en /" }
    if (requireHttps) {
        require(value.startsWith("https://")) { "$propertyName debe usar HTTPS" }
    }
    return value
}

fun String.asBuildConfigValue(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val developmentApiBaseUrl = apiBaseUrl(
    propertyName = "DEVELOPMENT_API_BASE_URL",
    defaultValue = "http://10.0.2.2:4000/api/"
)
val stagingApiBaseUrl = apiBaseUrl(
    propertyName = "STAGING_API_BASE_URL",
    defaultValue = "https://staging-api.urbify.example/api/",
    requireHttps = true
)
val productionApiBaseUrl = apiBaseUrl(
    propertyName = "PRODUCTION_API_BASE_URL",
    defaultValue = "https://api.urbify.example/api/",
    requireHttps = true
)

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
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", developmentApiBaseUrl.asBuildConfigValue())
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
            // Es el único entorno que puede apuntar al backend HTTP local.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "API_BASE_URL", stagingApiBaseUrl.asBuildConfigValue())
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", productionApiBaseUrl.asBuildConfigValue())
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
