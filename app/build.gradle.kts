import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// `local.properties` no se versiona y permite apuntar development a una IP LAN
// sin editar fuentes. Las propiedades -P tienen prioridad para CI/CD.
val localProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        FileInputStream(propsFile).use { load(it) }
    }
}

/** URLs configurables mediante -P<ENTORNO>_API_BASE_URL, gradle.properties o local.properties. */
fun apiBaseUrl(propertyName: String, defaultValue: String, requireHttps: Boolean = false): String {
    val value = providers.gradleProperty(propertyName).orNull
        ?: localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }
        ?: defaultValue
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
        // minSdk 24 no trae java.time (API 26+); MisSolicitudesClienteScreen
        // lo usa para formatear fechas, así que se desugariza en vez de
        // reescribirlo con SimpleDateFormat.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Sin esto, cualquier llamada a android.util.Log (u otro método del SDK
    // sin mockear) en un test unitario puro (no Robolectric) lanza
    // "Method ... not mocked". isReturnDefaultValues hace que esos stubs
    // devuelvan su valor por defecto (false/0/null) en vez de tirar.
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    // mockk-android trae transitivamente JUnit 5 (jupiter), que duplica
    // archivos META-INF/LICENSE* con otras libs de test al empaquetar el APK
    // de androidTest. Son metadata de licencia, no código: excluirlos del
    // empaquetado es la solución estándar de AGP para este conflicto.
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
            )
        }
    }
}

// runTest/UnconfinedTestDispatcher/setMain/resetMain (kotlinx-coroutines-test)
// son @ExperimentalCoroutinesApi; en vez de un @OptIn por archivo de test, se
// habilita acá para todo el módulo.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
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

tasks.matching {
    (it.name.startsWith("assemble") || it.name.startsWith("bundle")) &&
        it.name.endsWith("Release")
}
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
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.location)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)
    // MockK 1.13.13 trae transitivamente una versión de ByteBuddy que no
    // soporta JDKs muy nuevos (falla con "Java N is not supported by the
    // current version of Byte Buddy"). Forzamos una versión más nueva en el
    // classpath de test — Gradle resuelve a la más alta entre las
    // declaradas, así que esto sobreescribe la transitiva de MockK.
    testImplementation(libs.byte.buddy)
    testImplementation(libs.byte.buddy.agent)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
