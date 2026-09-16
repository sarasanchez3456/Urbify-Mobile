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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Verificación temprana y config-cache-friendly de la firma de release.
//
// A propósito NO usa gradle.taskGraph.whenReady: esa API captura una
// referencia viva al Project/TaskExecutionGraph dentro del closure, lo cual
// es frágil con la configuration cache (y está marcada para deprecación en
// builds futuras). En su lugar, registramos una tarea normal que resuelve y
// captura los valores durante la fase de configuración (variables locales
// Serializable, no objetos del modelo de Gradle) y sólo lee/valida esos
// valores dentro de doLast — el patrón recomendado por Gradle para tareas
// compatibles con configuration cache.
//
// El error nombra qué propiedades faltan (storeFile/storePassword/keyAlias/
// keyPassword) o si el archivo del keystore no existe/no se puede leer, pero
// nunca imprime valores de password/alias — sólo nombres de propiedades y la
// ruta (no secreta) del keystore.
val checkReleaseSigningConfig = tasks.register("checkReleaseSigningConfig") {
    group = "verification"
    description = "Falla con un mensaje claro si falta o es inválida la firma de release."

    val storeFilePath = releaseSigningValue("storeFile", "RELEASE_STORE_FILE")
    val hasStorePassword = releaseSigningValue("storePassword", "RELEASE_STORE_PASSWORD") != null
    val hasKeyAlias = releaseSigningValue("keyAlias", "RELEASE_KEY_ALIAS") != null
    val hasKeyPassword = releaseSigningValue("keyPassword", "RELEASE_KEY_PASSWORD") != null
    val resolvedStoreFile = storeFilePath?.let { rootProject.file(it) }

    doLast {
        val faltantes = buildList {
            if (storeFilePath == null) add("storeFile")
            if (!hasStorePassword) add("storePassword")
            if (!hasKeyAlias) add("keyAlias")
            if (!hasKeyPassword) add("keyPassword")
        }
        if (faltantes.isNotEmpty()) {
            throw GradleException(
                "Falta configurar la firma de release: ${faltantes.joinToString(", ")}. " +
                    "Completá keystore.properties (local, gitignored) o las variables de " +
                    "entorno RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS " +
                    "/ RELEASE_KEY_PASSWORD (CI). Ver docs/RELEASE_SIGNING.md."
            )
        }
        checkNotNull(resolvedStoreFile) // no debería poder ser null si faltantes está vacío
        if (!resolvedStoreFile.exists()) {
            throw GradleException(
                "El keystore de release configurado en storeFile no existe: " +
                    "${resolvedStoreFile.path}. Verificá la ruta en keystore.properties o en " +
                    "RELEASE_STORE_FILE."
            )
        }
        if (!resolvedStoreFile.canRead()) {
            throw GradleException(
                "El keystore de release configurado en storeFile no se puede leer " +
                    "(permisos del archivo): ${resolvedStoreFile.path}."
            )
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach { dependsOn(checkReleaseSigningConfig) }

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